package com.mayday.crawler.executor.download;

import com.mayday.crawler.service.ICrawlerImageService;
import com.mayday.crawler.util.RequestHeaderBuilder;
import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mayday.crawler.modl.entity.CrawlerTaskEntity;
import cn.hutool.crypto.digest.DigestUtil;
import org.jsoup.Jsoup;
import org.jsoup.Connection;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 异步图片下载器
 * 使用虚拟线程池批量并发下载图片，提升下载速度
 */
@Component
public class AsyncImageDownloader
{
    private static final Logger log = LoggerFactory.getLogger(AsyncImageDownloader.class);
    private final ICrawlerImageService imageService;
    private final ExecutorService downloadExecutor;
    private final Semaphore downloadSemaphore;
    
    // 下载统计
    private final AtomicInteger successCount = new AtomicInteger(0);
    private final AtomicInteger failedCount = new AtomicInteger(0);
    
    /**
     * 最大并发下载数
     */
    private static final int MAX_CONCURRENT_DOWNLOADS = 50;
    
    public AsyncImageDownloader(ICrawlerImageService imageService)
    {
        this.imageService = imageService;
        // 使用虚拟线程池，轻量级，支持大量并发
        this.downloadExecutor = Executors.newVirtualThreadPerTaskExecutor();
        this.downloadSemaphore = new Semaphore(MAX_CONCURRENT_DOWNLOADS);
    }
    
    /**
     * 批量异步下载图片
     * @param images 待下载的图片列表
     * @param task 爬虫任务
     * @param baseDir 基础存储目录
     * @param pageUrl 页面URL（用作Referer）
     * @return 成功下载的数量
     */
    public int downloadBatch(List<CrawlerImageEntity> images, 
                            CrawlerTaskEntity task, 
                            Path baseDir,
                            String pageUrl)
    {
        if (images == null || images.isEmpty())
        {
            return 0;
        }
        
        CountDownLatch latch = new CountDownLatch(images.size());
        AtomicInteger batchSuccess = new AtomicInteger(0);
        AtomicInteger batchFailed = new AtomicInteger(0);
        
        for (CrawlerImageEntity image : images)
        {
            // 提交到虚拟线程池异步下载
            downloadExecutor.submit(() -> {
                try
                {
                    // 限流：同时最多 MAX_CONCURRENT_DOWNLOADS 个下载任务
                    downloadSemaphore.acquire();
                    try
                    {
                        boolean success = downloadSingleImage(image, task, baseDir, pageUrl);
                        if (success)
                        {
                            batchSuccess.incrementAndGet();
                            successCount.incrementAndGet();
                        }
                        else
                        {
                            batchFailed.incrementAndGet();
                            failedCount.incrementAndGet();
                        }
                    }
                    finally
                    {
                        downloadSemaphore.release();
                    }
                }
                catch (Exception e)
                {
                    log.error("下载图片异常: {}", image.getUrl(), e);
                    batchFailed.incrementAndGet();
                    failedCount.incrementAndGet();
                }
                finally
                {
                    latch.countDown();
                }
            });
        }
        
        // 等待所有下载完成（最多等待5分钟）
        try
        {
            boolean finished = latch.await(5, TimeUnit.MINUTES);
            if (!finished)
            {
                log.warn("批量下载超时，部分图片可能未下载完成");
            }
        }
        catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            log.error("批量下载被中断", e);
        }
        
        log.info("批量下载完成: 成功={}, 失败={}", batchSuccess.get(), batchFailed.get());
        return batchSuccess.get();
    }
    
    /**
     * 下载单张图片
     */
    private boolean downloadSingleImage(CrawlerImageEntity image, 
                                       CrawlerTaskEntity task,
                                       Path baseDir,
                                       String pageUrl)
    {
        try
        {
            String imgUrl = image.getUrl();
            if (imgUrl == null || imgUrl.isEmpty())
            {
                updateImageStatus(image, "FAILED", "图片URL为空");
                return false;
            }
            
            // 创建存储目录
            Files.createDirectories(baseDir);
            
            // 生成文件名
            String extension = extractFileExtension(imgUrl);
            String fileName = image.getId() + (extension != null && !"unknown".equals(extension) ? "." + extension : ".jpg");
            Path targetFile = baseDir.resolve(fileName);
            
            // 如果文件已存在且大小>0，跳过下载
            if (Files.exists(targetFile) && Files.size(targetFile) > 0)
            {
                log.debug("图片已存在，跳过下载: {}", fileName);
                updateImageStatusSuccess(image, targetFile, fileName);
                return true;
            }
            
            // 构建下载请求
            int timeout = task.getRequestTimeout() != null ? task.getRequestTimeout() : 30000;
            Connection connection = Jsoup.connect(imgUrl)
                    .timeout(timeout)
                    .ignoreContentType(true)
                    .maxBodySize(20 * 1024 * 1024)  // 最大20MB
                    .followRedirects(true);
            
            // 使用 RequestHeaderBuilder 设置请求头（模拟真实浏览器）
            RequestHeaderBuilder headerBuilder = RequestHeaderBuilder.create()
                    .asImageRequest();  // 设置为图片请求模式
            
            // 设置 Referer
            if (task.getReferer() != null && !task.getReferer().isEmpty())
            {
                headerBuilder.withReferer(task.getReferer());
            }
            else if (pageUrl != null && !pageUrl.isEmpty())
            {
                headerBuilder.withReferer(pageUrl);
            }
            
            // 应用请求头
            headerBuilder.applyTo(connection);
            
            // 重试机制（最多3次）
            int maxRetries = 3;
            byte[] bytes = null;
            String contentType = null;
            int statusCode = 0;
            
            for (int retry = 0; retry <= maxRetries; retry++)
            {
                try
                {
                    Connection.Response response = connection.execute();
                    statusCode = response.statusCode();
                    contentType = response.contentType();
                    
                    if (statusCode == 200)
                    {
                        bytes = response.bodyAsBytes();
                        break;  // 成功，跳出重试循环
                    }
                    else
                    {
                        log.warn("下载图片HTTP错误 (尝试 {}/{}): {} - 状态码: {}", 
                                retry + 1, maxRetries + 1, imgUrl, statusCode);
                    }
                }
                catch (Exception e)
                {
                    if (retry < maxRetries)
                    {
                        log.warn("下载图片失败 (尝试 {}/{}): {} - 错误: {}", 
                                retry + 1, maxRetries + 1, imgUrl, e.getMessage());
                        // 指数退避：等待1s, 2s, 4s
                        Thread.sleep((long) Math.pow(2, retry) * 1000);
                    }
                    else
                    {
                        throw e;  // 最后一次重试失败，抛出异常
                    }
                }
            }
            
            // 检查HTTP状态码
            if (statusCode != 200)
            {
                updateImageStatus(image, "FAILED", "HTTP状态码: " + statusCode);
                return false;
            }
            
            // 检查Content-Type
            if (contentType == null || !contentType.toLowerCase().startsWith("image/"))
            {
                log.warn("非图片Content-Type: {} - {}", imgUrl, contentType);
                // 不严格要求Content-Type，某些网站可能返回错误的类型
            }
            
            // 检查响应内容
            if (bytes == null || bytes.length == 0)
            {
                updateImageStatus(image, "FAILED", "图片内容为空");
                return false;
            }
            
            // 检查文件大小（过滤过小的图片，可能是占位图）
            if (bytes.length < 1024)  // 小于1KB
            {
                log.warn("图片过小，可能是占位图: {} - {}字节", imgUrl, bytes.length);
                updateImageStatus(image, "FAILED", "图片过小: " + bytes.length + "字节");
                return false;
            }
            
            // 写入文件
            Files.write(targetFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            
            // 更新数据库记录
            updateImageStatusSuccess(image, targetFile, fileName);
            
            log.debug("图片下载成功: {} -> {} ({}字节)", imgUrl, fileName, bytes.length);
            return true;
        }
        catch (Exception e)
        {
            log.warn("下载图片失败: {} - {}", image.getUrl(), e.getMessage());
            updateImageStatus(image, "FAILED", e.getMessage());
            return false;
        }
    }
    
    /**
     * 更新图片状态为成功
     */
    private void updateImageStatusSuccess(CrawlerImageEntity image, Path targetFile, String fileName)
    {
        try
        {
            byte[] bytes = Files.readAllBytes(targetFile);
            image.setFileName(fileName);
            image.setFilePath(targetFile.getParent().toString());
            image.setFileSize((long) bytes.length);
            image.setMd5(DigestUtil.md5Hex(bytes));
            image.setDownloadStatus("SUCCESS");
            image.setErrorMsg(null);
            imageService.updateById(image);
        }
        catch (Exception e)
        {
            log.error("更新图片状态失败: {}", image.getId(), e);
        }
    }
    
    /**
     * 更新图片状态为失败
     */
    private void updateImageStatus(CrawlerImageEntity image, String status, String errorMsg)
    {
        try
        {
            image.setDownloadStatus(status);
            image.setErrorMsg(errorMsg);
            imageService.updateById(image);
        }
        catch (Exception e)
        {
            log.error("更新图片状态失败: {}", image.getId(), e);
        }
    }
    
    /**
     * 提取文件扩展名
     */
    private String extractFileExtension(String url)
    {
        if (url == null || url.isEmpty())
        {
            return "jpg";
        }
        
        // 移除查询参数
        String path = url.split("\\?")[0];
        
        int lastDot = path.lastIndexOf('.');
        int lastSlash = path.lastIndexOf('/');
        
        if (lastDot > lastSlash && lastDot < path.length() - 1)
        {
            String ext = path.substring(lastDot + 1).toLowerCase();
            // 验证扩展名是否合法
            if (ext.matches("[a-z0-9]{2,5}"))
            {
                return ext;
            }
        }
        
        return "jpg";  // 默认jpg
    }
    
    /**
     * 获取下载统计
     */
    public DownloadStats getStats()
    {
        return new DownloadStats(successCount.get(), failedCount.get());
    }
    
    /**
     * 重置统计
     */
    public void resetStats()
    {
        successCount.set(0);
        failedCount.set(0);
    }
    
    /**
     * 下载统计
     */
    public record DownloadStats(int success, int failed)
    {
        public int total()
        {
            return success + failed;
        }
        
        public double successRate()
        {
            int total = total();
            return total > 0 ? (double) success / total * 100 : 0;
        }
    }
    
    /**
     * 关闭下载器
     */
    public void shutdown()
    {
        downloadExecutor.shutdown();
        try
        {
            if (!downloadExecutor.awaitTermination(60, TimeUnit.SECONDS))
            {
                downloadExecutor.shutdownNow();
            }
        }
        catch (InterruptedException e)
        {
            downloadExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}






