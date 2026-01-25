package com.mayday.crawler.executor;

import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mayday.crawler.modl.entity.CrawlerLogEntity;
import com.mayday.crawler.modl.entity.CrawlerTaskEntity;
import com.mayday.crawler.service.ICrawlerArticleService;
import com.mayday.crawler.service.ICrawlerImageService;
import com.mayday.crawler.service.ICrawlerLogService;
import com.mayday.crawler.service.ICrawlerProxyService;
import com.mayday.crawler.service.ICrawlerTaskService;
import cn.hutool.crypto.digest.DigestUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ThreadLocalRandom;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.mayday.common.sse.SsePublisher;

/**
 * 爬虫执行器
 */
@Component
public class CrawlerExecutor
{
    private static final Logger log = LoggerFactory.getLogger(CrawlerExecutor.class);
    private final ICrawlerTaskService taskService;
    private final ICrawlerArticleService articleService;
    private final ICrawlerImageService imageService;
    private final ICrawlerLogService logService;
    private final ICrawlerProxyService proxyService;
    private final SsePublisher ssePublisher;
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * SSE主题：任务状态
     */
    private static final String SSE_TOPIC_TASK_STATUS = "crawler-task-status";

    /**
     * 图片存储根目录（兼容两种配置键）
     * - crawler.image.base-path
     * - crawler.image-base-path
     */
    @Value("${crawler.image.base-path:${crawler.image-base-path:./data/crawler-images}}")
    private String imageBasePath;
    
    public CrawlerExecutor(@Lazy ICrawlerTaskService taskService,
                          ICrawlerArticleService articleService,
                          ICrawlerImageService imageService,
                          ICrawlerLogService logService,
                          ICrawlerProxyService proxyService,
                          SsePublisher ssePublisher)
    {
        this.taskService = taskService;
        this.articleService = articleService;
        this.imageService = imageService;
        this.logService = logService;
        this.proxyService = proxyService;
        this.ssePublisher = ssePublisher;
    }
    
    // 任务执行状态管理
    private final Map<Long, Boolean> runningTasks = new ConcurrentHashMap<>();
    private final Map<Long, Boolean> proxyDecisionLoggedTasks = new ConcurrentHashMap<>();
    private final Map<Long, Integer> listPagesProcessed = new ConcurrentHashMap<>();
    // 文章ID -> 图片存储目录名 缓存
    private final Map<Long, String> articleImageFolderCache = new ConcurrentHashMap<>();
    
    // User-Agent池（用于反爬虫）
    private static final String[] USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    };
    
    // 随机数生成器（用于UA轮换）
    private final Random random = new Random();
    
    /**
     * 异步执行爬虫任务
     */
    @Async("crawlerTaskExecutor")
    public void executeTask(Long taskId)
    {
        // 如果任务已在运行中，先清理旧状态（可能是异常退出导致的残留）
        if (runningTasks.containsKey(taskId))
        {
            log.warn("任务 {} 已在运行中，清理旧状态后重新启动", taskId);
            runningTasks.remove(taskId);
            // 等待一小段时间，确保旧任务完全停止
            try
            {
                Thread.sleep(500);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
        }
        
        runningTasks.put(taskId, true);
        
        try
        {
            CrawlerTaskEntity task = taskService.getById(taskId);
            if (task == null)
            {
                log.error("任务 {} 不存在", taskId);
                return;
            }
            
            log.info("开始执行爬虫任务: {}", task.getTaskName());
            addLog(taskId, "INFO", "任务开始执行", "任务开始执行");
            
            // 解析起始URL
            List<String> startUrls = parseStartUrls(task.getStartUrls());
            if (startUrls.isEmpty())
            {
                addLog(taskId, "ERROR", "起始URL为空", "起始URL为空，无法执行任务");
                updateTaskStatus(taskId, "ERROR", "起始URL为空");
                return;
            }
            // 初始化统计
            AtomicInteger totalUrls = new AtomicInteger(0);
            AtomicInteger crawledUrls = new AtomicInteger(0);
            AtomicInteger successCount = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            
            // URL队列和已访问集合（使用线程安全的集合）
            Queue<UrlInfo> urlQueue = new ConcurrentLinkedQueue<>();
            Set<String> visitedUrls = ConcurrentHashMap.newKeySet();
            
            // 添加起始URL到队列
            for (String url : startUrls)
            {
                urlQueue.offer(new UrlInfo(url, 0));
                visitedUrls.add(normalizeUrl(url));
            }
            
            totalUrls.set(urlQueue.size());
            task.setTotalUrls(totalUrls.get());
            taskService.updateById(task);
            
            // 立即推送任务开始状态（确保前端即时收到"运行中"状态）
            publishTaskStatus(task, "RUNNING");
            
            // 获取基础URL（用于判断是否在同一站点）
            String baseUrl = extractBaseUrl(startUrls.getFirst());
            
            // 开始爬取
            int maxDepth = task.getMaxDepth() != null ? task.getMaxDepth() : 3;
            String scopeType = task.getScopeType() != null ? task.getScopeType() : "SITE";
            
            // 限制最大URL数量，防止无限循环
            int maxUrls = 10000; // 最大爬取URL数量
            AtomicInteger consecutiveFailures = new AtomicInteger(0); // 连续失败次数（线程安全）
            int maxConsecutiveFailures = 10; // 最大连续失败次数
            
            // 记录任务开始时间
            long taskStartTime = System.currentTimeMillis();
            
            // 使用虚拟线程池进行并发处理，提升爬取速度
            int concurrency = 10; // 并发数
            Semaphore semaphore = new Semaphore(concurrency);
            ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
            List<Future<Boolean>> futures = new ArrayList<>();
            
            log.info("任务 {} 开始执行，使用虚拟线程并发处理，并发数: {}", taskId, concurrency);
            addLog(taskId, "INFO", "任务开始", String.format("开始执行爬虫任务，并发数: %d", concurrency));
            
            // 并发处理URL队列
            AtomicInteger activeTasks = new AtomicInteger(0);
            
            while ((!urlQueue.isEmpty() || activeTasks.get() > 0) && runningTasks.getOrDefault(taskId, false))
            {
                // 检查是否超过最大URL数量
                if (crawledUrls.get() >= maxUrls)
                {
                    break;
                }
                
                // 检查停止标志
                if (!runningTasks.getOrDefault(taskId, false))
                {
                    break;
                }
                
                // 提交新的任务到线程池（如果队列中有URL且未达到并发限制）
                while (!urlQueue.isEmpty() && activeTasks.get() < concurrency && runningTasks.getOrDefault(taskId, false))
                {
                    UrlInfo urlInfo = urlQueue.poll();
                    if (urlInfo == null) break;
                    
                    // 检查深度
                    if (urlInfo.depth > maxDepth)
                    {
                        continue;
                    }
                    
                    // 检查站点范围
                    if ("SITE".equals(scopeType))
                    {
                        String currentBaseUrl = extractBaseUrl(urlInfo.url);
                        if (!baseUrl.equals(currentBaseUrl))
                        {
                            continue;
                        }
                    }
                    
                    // 提交到虚拟线程池
                    activeTasks.incrementAndGet();
                    Future<Boolean> future = executor.submit(() -> {
                        try
                        {
                            // 请求间隔（在虚拟线程中等待，不阻塞主线程）
                            if (crawledUrls.get() > 0)
                            {
                                long interval = task.getRequestInterval() != null ? task.getRequestInterval() : 1000;
                                if (task.getRandomInterval() != null && task.getRandomInterval() == 1)
                                {
                                    interval = (long)(interval * (0.5 + Math.random()));
                                }
                                Thread.sleep(interval);
                            }
                            
                            // 检查停止标志
                            if (!runningTasks.getOrDefault(taskId, false))
                            {
                                return false;
                            }
                            
                            // 执行爬取
                            boolean success = crawlUrl(task, urlInfo, urlQueue, visitedUrls, baseUrl, scopeType, maxDepth, 
                                    totalUrls, crawledUrls, successCount, errorCount, maxUrls);
                            
                            crawledUrls.incrementAndGet();
                            
                            // 更新任务进度（前20个URL每个都推送，之后每5个推送一次）
                            if (crawledUrls.get() <= 20 || crawledUrls.get() % 5 == 0)
                            {
                                updateTaskProgress(taskId, totalUrls.get(), crawledUrls.get(), 
                                        successCount.get(), errorCount.get());
                            }
                            
                            return success;
                        }
                        catch (Exception e)
                        {
                            errorCount.incrementAndGet();
                            crawledUrls.incrementAndGet();
                            log.error("爬取URL失败: {}", urlInfo.url, e);
                            addLog(taskId, "ERROR", "爬取失败", 
                                    String.format("URL: %s, 错误: %s", urlInfo.url, 
                                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
                            return false;
                        }
                        finally
                        {
                            activeTasks.decrementAndGet();
                        }
                    });
                    
                    futures.add(future);
                }
                
                // 清理已完成的任务
                futures.removeIf(f -> {
                    if (f.isDone())
                    {
                        try
                        {
                            f.get(); // 获取结果，如果有异常会抛出
                        }
                        catch (Exception e)
                        {
                            // 异常已在任务内部处理
                        }
                        return true;
                    }
                    return false;
                });
                
                // 短暂休眠，避免CPU占用过高
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // 等待所有任务完成
            log.info("任务 {} 等待所有并发任务完成...", taskId);
            for (Future<Boolean> future : futures)
            {
                try
                {
                    future.get(30, TimeUnit.SECONDS);
                }
                catch (Exception e)
                {
                    log.warn("任务 {} 等待任务完成时出错: {}", taskId, e.getMessage());
                }
            }
            
            // 关闭线程池
            executor.shutdown();
            try
            {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS))
                {
                    executor.shutdownNow();
                }
            }
            catch (InterruptedException e)
            {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
            
            // 计算任务耗时
            long taskEndTime = System.currentTimeMillis();
            long taskDuration = taskEndTime - taskStartTime;
            long taskDurationSeconds = taskDuration / 1000;
            long taskDurationMinutes = taskDurationSeconds / 60;
            String durationStr = taskDurationMinutes > 0 
                    ? String.format("%d分%d秒", taskDurationMinutes, taskDurationSeconds % 60)
                    : String.format("%d秒", taskDurationSeconds);
            
            // 更新最终状态
            boolean wasRunning = runningTasks.get(taskId);
            if (wasRunning)
            {
                updateTaskStatus(taskId, "COMPLETED", null);
                updateTaskProgress(taskId, totalUrls.get(), crawledUrls.get(), 
                        successCount.get(), errorCount.get());
                
                String summary = String.format(
                    "任务完成！总URL数: %d, 已爬取: %d, 成功: %d, 失败: %d, 耗时: %s",
                    totalUrls.get(), crawledUrls.get(), successCount.get(), errorCount.get(), durationStr
                );
                
                log.info("任务 {} {}", taskId, summary);
                addLog(taskId, "INFO", "任务完成", summary);
            }
            else
            {
                updateTaskStatus(taskId, "STOPPED", null);
                String summary = String.format(
                    "任务已停止！总URL数: %d, 已爬取: %d, 成功: %d, 失败: %d, 耗时: %s",
                    totalUrls.get(), crawledUrls.get(), successCount.get(), errorCount.get(), durationStr
                );
                
                log.info("任务 {} {}", taskId, summary);
                addLog(taskId, "INFO", "任务已停止", summary);
            }
        }
        catch (Exception e)
        {
            log.error("执行爬虫任务失败: {}", taskId, e);
            updateTaskStatus(taskId, "ERROR", e.getMessage());
            addLog(taskId, "ERROR", "任务执行异常", e.getMessage());
        }
        finally
        {
            runningTasks.remove(taskId);
            proxyDecisionLoggedTasks.remove(taskId);
            listPagesProcessed.remove(taskId);
        }
    }
    
    /**
     * 爬取单个URL
     * @return true表示成功，false表示失败
     */
    private boolean crawlUrl(CrawlerTaskEntity task, UrlInfo urlInfo, Queue<UrlInfo> urlQueue,
                          Set<String> visitedUrls, String baseUrl, String scopeType, int maxDepth,
                          AtomicInteger totalUrls, AtomicInteger crawledUrls,
                          AtomicInteger successCount, AtomicInteger errorCount, int maxUrls)
    {
        // 人工延时，增加任务可视化效果 (200-500ms)
        // 这有助于防止任务在前端看起来像是瞬间完成没有进度
        try {
            long artificialDelay = 200 + java.util.concurrent.ThreadLocalRandom.current().nextInt(300);
            Thread.sleep(artificialDelay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        int maxRetries = task.getMaxRetries() != null ? task.getMaxRetries() : 3;
        // 对于连接超时错误，限制最多重试2次（包括第一次尝试，总共3次）
        int maxConnectRetries = 2;
        int timeout = task.getRequestTimeout() != null ? task.getRequestTimeout() : 30000; // 默认30秒超时
        
        // 解析代理列表（任务优先，其次全局）
        EffectiveProxyList effectiveProxyList = parseProxyList(task);
        if (!proxyDecisionLoggedTasks.containsKey(task.getId()))
        {
            proxyDecisionLoggedTasks.put(task.getId(), true);
            if (task.getUseProxy() != null && task.getUseProxy() == 1)
            {
                String sample = effectiveProxyList.proxies.isEmpty()
                        ? "NONE"
                        : String.format("%s %s:%d", effectiveProxyList.proxies.get(0).type, effectiveProxyList.proxies.get(0).host, effectiveProxyList.proxies.get(0).port);
                log.info("任务 {} 代理开关=1，代理来源={}，可用数量={}，示例={}",
                        task.getId(), effectiveProxyList.source, effectiveProxyList.proxies.size(), sample);
            }
            else
            {
                log.info("任务 {} 代理开关!=1（useProxy={}），将使用直连请求", task.getId(), task.getUseProxy());
            }
        }
        
        // SOCKS 代理下部分站点会出现 TLS 握手被终止（SSLHandshakeException）。
        // Clash mixed-port 同时支持 SOCKS/HTTP，遇到该问题时自动降级为 HTTP CONNECT 代理（同 host:port）。
        boolean forceHttpProxyForTls = false;

        // 重试机制
        for (int retry = 0; retry <= maxRetries; retry++)
        {
            // 每次重试前检查停止标志
            if (!runningTasks.getOrDefault(task.getId(), false))
            {
                log.info("任务 {} 已收到停止请求，停止当前URL的爬取: {}", task.getId(), urlInfo.url);
                return false;
            }
            
            ProxyInfo requestProxyInfo = null;
            String requestProxyTypeForThisAttempt = null;
            try
            {
                // 构建请求连接，增强反爬虫能力
                org.jsoup.Connection connection = Jsoup.connect(urlInfo.url)
                        .userAgent(getUserAgent(task, retry))
                        .timeout(timeout) // 连接和读取超时
                        .followRedirects(true) // 允许重定向
                        .maxBodySize(10 * 1024 * 1024); // 限制响应体大小，防止内存溢出
                        // 注意：ignoreContentType 和 ignoreHttpErrors 在最后设置
                
                // 设置代理（如果启用）
                if (task.getUseProxy() != null && task.getUseProxy() == 1)
                {
                    if (effectiveProxyList.proxies.isEmpty())
                    {
                        // 任务启用代理但未配置/无启用全局代理：明确提示
                        if (retry == 0)
                        {
                            log.warn("任务 {} 已启用代理(useProxy=1)，但未找到可用代理（source={}）。本次请求将直连，可能导致 ConnectException。", task.getId(), effectiveProxyList.source);
                            addLog(task.getId(), "WARN", "代理未生效",
                                    "任务已启用代理(useProxy=1)，但未找到可用代理。请到「爬虫管理-代理配置」启用至少一条代理，" +
                                            "或在任务中填写 proxyList。注意：代理的 host/port 需要在『后端服务运行的机器』上可访问。");
                        }
                    }
                    else
                    {
                        ProxyInfo proxyInfo = effectiveProxyList.proxies.get(ThreadLocalRandom.current().nextInt(effectiveProxyList.proxies.size()));
                        requestProxyInfo = proxyInfo;
                        requestProxyTypeForThisAttempt = forceHttpProxyForTls ? "HTTP" : proxyInfo.type;
                        Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(requestProxyTypeForThisAttempt) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                        Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyInfo.host, proxyInfo.port));
                        connection.proxy(proxy);

                        if (retry == 0)
                        {
                            log.info("任务 {} 本次请求使用{}代理({}) {}:{}",
                                    task.getId(),
                                    effectiveProxyList.source,
                                    requestProxyTypeForThisAttempt,
                                    proxyInfo.host,
                                    proxyInfo.port);
                        }

                        if (proxyInfo.username != null && !proxyInfo.username.isEmpty())
                        {
                            // 代理认证：通过全局 Authenticator 处理（对 HttpURLConnection/Jsoup 生效）
                            String password = proxyInfo.password != null ? proxyInfo.password : "";
                            Authenticator.setDefault(new Authenticator()
                            {
                                @Override
                                protected PasswordAuthentication getPasswordAuthentication()
                                {
                                    return new PasswordAuthentication(proxyInfo.username, password.toCharArray());
                                }
                            });
                        }
                    }
                }
                
                // 先添加自定义请求头（在设置Referer之前）
                addCustomHeaders(connection, task);
                
                // 设置Referer（必须在添加请求头之后）
                if (task.getReferer() != null && !task.getReferer().isEmpty())
                {
                    connection.referrer(task.getReferer());
                }
                else
                {
                    // 如果没有配置Referer，使用当前URL的基础URL（模拟从首页访问）
                    String refererUrl = extractBaseUrl(urlInfo.url);
                    connection.referrer(refererUrl);
                }
                
                // 添加Cookie
                addCookies(connection, task);
                
                // 忽略内容类型错误（某些网站返回的Content-Type可能不正确）
                connection.ignoreContentType(true);
                // 忽略HTTP错误（某些网站可能返回非200状态码但内容正常）
                connection.ignoreHttpErrors(true);
                
                // 执行请求前再次检查停止标志（避免执行长时间的网络请求）
                if (!runningTasks.getOrDefault(task.getId(), false))
                {
                    log.info("任务 {} 已收到停止请求，取消网络请求: {}", task.getId(), urlInfo.url);
                    return false;
                }
                
                // 执行请求并获取响应
                org.jsoup.Connection.Response response = connection.execute();
                // 成功后清理 TLS 降级标志
                forceHttpProxyForTls = false;
                
                // 检查HTTP状态码
                int statusCode = response.statusCode();
                if (statusCode >= 400)
                {
                    log.warn("请求URL返回错误状态码 {}: {}", statusCode, urlInfo.url);
                    // 对于4xx和5xx错误，记录但不重试（可能是权限问题或服务器错误）
                    addLog(task.getId(), "WARN", "HTTP错误", 
                            String.format("URL: %s, 状态码: %d", urlInfo.url, statusCode));
                    return false;
                }
                
                Document doc = response.parse();
            
            // 解析文档后检查停止标志
            if (!runningTasks.getOrDefault(task.getId(), false))
            {
                log.info("任务 {} 已收到停止请求，停止处理页面内容: {}", task.getId(), urlInfo.url);
                return false;
            }
            
            // 判断页面类型：列表页 or 详情页
            PageType pageType = detectPageType(doc, urlInfo.url);
            log.debug("URL {} 被判断为页面类型: {}", urlInfo.url, pageType.name());
            
            // 根据爬取类型和页面类型处理
            String crawlType = task.getCrawlType();
            boolean hasContent = false;
            Long articleId = null;
            
            if ("LIST".equals(pageType.name()))
            {
                // 列表页：只提取文章链接，不提取图片
                // 防止递归：如果URL深度>0且看起来像详情页（.html结尾），不应该再提取文章链接
                // 这通常意味着这个页面是从列表页提取出来的"文章链接"，不应该再被当作列表页处理
                boolean shouldExtractLinks = true;
                if (urlInfo.depth > 0 && urlInfo.url.matches(".*\\.html$|.*/\\d+\\.html$"))
                {
                    // URL看起来像详情页，但被误判为列表页，强制当作详情页处理，不提取链接
                    log.warn("URL {} 看起来像详情页但被判断为列表页，强制当作详情页处理", urlInfo.url);
                    shouldExtractLinks = false;
                    // 强制当作详情页处理，继续执行详情页逻辑
                    pageType = PageType.DETAIL;
                }
                
                if (shouldExtractLinks)
                {
                    // 提取链接前检查停止标志
                    if (!runningTasks.getOrDefault(task.getId(), false))
                    {
                        log.info("任务 {} 已收到停止请求，跳过提取文章链接", task.getId());
                        return true; // 返回true表示已处理（虽然没提取链接）
                    }
                    
                    // 列表页：提取文章链接
                    extractArticleLinks(task, urlInfo.url, doc, urlQueue, visitedUrls, 
                            baseUrl, scopeType, maxDepth, totalUrls, maxUrls);
                    
                    // 提取链接后检查停止标志
                    if (!runningTasks.getOrDefault(task.getId(), false))
                    {
                        log.info("任务 {} 已收到停止请求，跳过翻页处理", task.getId());
                        return true;
                    }
                    
                    // 列表页：按配置自动跟随下一页
                    enqueueNextListPageIfNeeded(task, urlInfo, doc, urlQueue, visitedUrls, baseUrl, scopeType, totalUrls, maxUrls);
                }
                hasContent = true;
            }
            
            if ("DETAIL".equals(pageType.name()) || "MIXED".equals(pageType.name()))
            {
                // 详情页或混合页：提取文章内容和图片
                // 注意：MIXED 类型通常表示页面既有列表又有详情，但根据URL判断，如果是 /page/ 开头的，应该只提取链接
                // 这里只处理详情页逻辑，列表页逻辑在上面已经处理了
                if ("MIXED".equals(pageType.name()) && urlInfo.url.contains("/page/"))
                {
                    // MIXED 类型但URL包含 /page/，应该只当作列表页处理（已在上面处理）
                    // 这里不做任何处理，避免重复提取
                }
                else
                {
                    // 详情页或MIXED类型（非列表页URL）：提取文章内容和图片
                    if ("ARTICLE".equals(crawlType) || "BOTH".equals(crawlType))
                    {
                        try
                        {
                            articleId = extractArticle(task, urlInfo.url, doc, successCount);
                            if (articleId != null)
                            {
                                hasContent = true;
                            }
                        }
                        catch (Exception e)
                        {
                            log.error("提取文章失败: {}", urlInfo.url, e);
                        }
                    }
                    
                    // 在详情页提取图片并关联到文章
                    if ("IMAGE".equals(crawlType) || "BOTH".equals(crawlType))
                    {
                        // 提取图片前检查停止标志
                        if (!runningTasks.getOrDefault(task.getId(), false))
                        {
                            log.info("任务 {} 已收到停止请求，跳过提取图片", task.getId());
                            return true; // 返回true表示已处理（虽然没提取图片）
                        }
                        
                        try
                        {
                            log.info("开始从详情页 {} 提取图片（articleId: {}）", urlInfo.url, articleId);
                            extractImagesFromDetailPage(task, articleId, urlInfo.url, doc, successCount);
                            hasContent = true;
                        }
                        catch (Exception e)
                        {
                            log.error("从详情页提取图片失败: {}", urlInfo.url, e);
                        }
                    }
                    else
                    {
                        log.debug("爬取类型为 {}，跳过详情页图片提取", crawlType);
                    }
                }
            }
            
            // 如果不是列表页或详情页，尝试提取所有链接（兜底逻辑）
            if (!hasContent && urlInfo.depth < maxDepth && totalUrls.get() < maxUrls)
            {
                extractAllLinks(doc, urlQueue, visitedUrls, baseUrl, scopeType, 
                        urlInfo.depth, maxDepth, totalUrls, maxUrls);
            }
            
                // 如果成功获取页面内容或提取到数据，认为成功
                if (hasContent || doc != null)
                {
                    successCount.incrementAndGet();
                    return true;
                }
                else
                {
                    return false;
                }
            }
            catch (java.net.SocketTimeoutException | java.net.ConnectException | 
                   java.net.UnknownHostException | SSLException e)
            {
                // 若使用 SOCKS 代理发生 TLS 握手错误，尝试切换为 HTTP 代理重试
                if (!forceHttpProxyForTls
                        && requestProxyInfo != null
                        && "SOCKS".equalsIgnoreCase(requestProxyTypeForThisAttempt)
                        && (e instanceof SSLHandshakeException
                        || (e.getMessage() != null && e.getMessage().toLowerCase().contains("handshake"))))
                {
                    forceHttpProxyForTls = true;
                    log.warn("任务 {} 走 SOCKS 代理发生 TLS 握手失败，下一次尝试将降级为 HTTP 代理: {}:{}（错误: {}）",
                            task.getId(),
                            requestProxyInfo.host,
                            requestProxyInfo.port,
                            e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                    continue;
                }

                // 网络超时、连接错误、DNS错误或SSL错误，快速失败（限制重试次数）
                // retry=0是第一次尝试，retry=1是第1次重试，retry=2是第2次重试
                // 对于连接超时，最多只重试2次（retry=1,2），总共尝试3次
                if (retry < maxConnectRetries)
                {
                    // retry=0 表示第一次尝试失败，此处即将开始第 1 次重试
                    log.warn("请求URL失败，将进行第 {} 次重试: {} (错误: {})",
                            retry + 1, urlInfo.url, e.getClass().getSimpleName());
                    try
                    {
                        // 超时错误快速跳过，不等待太久：2s, 4s
                        Thread.sleep(2000L * (retry + 1));
                    }
                    catch (InterruptedException ie)
                    {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    continue; // 继续重试
                }
                else
                {
                    // 重试次数用完，快速跳过该URL
                    // retry从0开始，所以实际重试次数是retry（retry=1是第1次重试，retry=2是第2次重试）
                    int actualRetries = Math.min(retry, maxConnectRetries); // 实际重试次数
                    String errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                    log.warn("请求URL失败（已重试{}次），跳过: {} (错误: {})", 
                            actualRetries, urlInfo.url, errorMsg);
                    // 记录到任务日志，但不记录完整堆栈（避免日志过多）
                    addLog(task.getId(), "WARN", "URL访问失败", 
                            String.format("URL: %s, 错误: %s (已重试%d次)", 
                                    urlInfo.url, errorMsg, actualRetries));
                    addLog(task.getId(), "WARN", "连通性提示",
                            "若浏览器可打开但后端爬虫 ConnectException，通常是『后端运行环境』没有走到代理/网络路由不同。请确认：\n" +
                                    "1) 任务 useProxy=1\n" +
                                    "2) 全局代理已启用，且 host/port 在后端机器上可访问（例如 Clash 在本机则填 127.0.0.1:7897；若后端在服务器上则需要服务器能访问该代理）\n" +
                                    "3) Clash mixed port 多数可用 SOCKS/HTTP，优先尝试 SOCKS。");
                    return false; // 快速失败，不阻塞任务
                }
            }
            catch (IOException e)
            {
                // 其他IO错误，根据错误类型决定是否重试
                String errorMsg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
                if (retry < Math.min(maxRetries, 1) && (errorMsg.contains("timeout") || errorMsg.contains("connection")))
                {
                    log.warn("请求URL IO错误，第 {} 次重试: {} (错误: {})", 
                            retry + 1, urlInfo.url, e.getClass().getSimpleName());
                    try
                    {
                        Thread.sleep(2000);
                    }
                    catch (InterruptedException ie)
                    {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                    continue;
                }
                else
                {
                    // 不增加errorCount，快速跳过
                    log.warn("请求URL失败，跳过: {} (错误: {})", urlInfo.url, e.getClass().getSimpleName());
                    addLog(task.getId(), "WARN", "URL访问失败", 
                            String.format("URL: %s, 错误: %s", urlInfo.url, e.getClass().getSimpleName()));
                    return false;
                }
            }
            catch (Exception e)
            {
                // 其他错误，不重试，快速跳过
                log.warn("处理URL失败，跳过: {} (错误: {})", urlInfo.url, e.getClass().getSimpleName());
                addLog(task.getId(), "WARN", "URL处理失败", 
                        String.format("URL: %s, 错误: %s", urlInfo.url, e.getClass().getSimpleName()));
                return false;
            }
        }
        
        // 所有重试都失败
        errorCount.incrementAndGet();
        return false;
    }
    
    /**
     * 检测页面类型：列表页、详情页或混合页
     */
    private PageType detectPageType(Document doc, String url)
    {
        // 0) 搜索结果页快速识别（WordPress 常见：/?s=keyword）
        // 这类页面是列表页，不应当作详情页提取正文图片
        try
        {
            if (url != null)
            {
                URI u = new URI(url);
                String query = u.getQuery();
                if (query != null && query.contains("s="))
                {
                    return PageType.LIST;
                }
            }
        }
        catch (Exception ignored)
        {
            // ignore
        }

        // 0) WordPress 归档/分类页快速识别：这类页面通常是列表页（如 ouxpa.com 分类页）
        // body class 常见包含 archive/category/tag 等
        try
        {
            Element body = doc.body();
            if (body != null)
            {
                String bodyClass = body.className();
                if (bodyClass != null)
                {
                    String c = bodyClass.toLowerCase();
                    if (c.contains("archive") || c.contains("category") || c.contains("tag") || c.contains("search"))
                    {
                        // 归档页基本可视为列表页（即使页面上也有 h1 / meta 等“详情信号”）
                        return PageType.LIST;
                    }
                }
            }
        }
        catch (Exception ignored)
        {
            // ignore
        }

        // 检测详情页特征
        Elements articleElements = doc.select("article, .article-content, .post-content, .content-detail, .entry-content");
        boolean hasArticleContent = articleElements.size() > 0;
        // 详情页通常只有一个主要内容区域
        boolean hasSingleArticleContent = articleElements.size() == 1;
        boolean hasSingleTitle = doc.select("h1").size() == 1;
        boolean hasArticleMeta = doc.select(".article-meta, .post-meta, .publish-time, time[datetime]").size() > 0;
        
        // 检测列表页特征
        boolean hasMultipleLinks = doc.select("a[href]").size() > 10;
        // 传统列表容器
        boolean hasListContainer = doc.select(".article-list, .post-list, .news-list, ul.article-list, .list-item").size() > 0;
        // 图片卡片网格布局（如 jrants.com）：多个 article.post 或 .entry 容器
        Elements cardContainers = doc.select("article.post, article.entry, .post, .entry, .card, .post-item, .entry-item");
        boolean hasMultipleCards = cardContainers.size() > 1; // 列表页通常有多个卡片
        boolean hasPagination = doc.select(".pagination, .page-nav, .pager, a[rel='next'], a:contains(Next), a:contains(下一页)").size() > 0;
        
        // URL模式检测（优先判断，URL模式是最可靠的信号）
        boolean urlLooksLikeDetail = url.matches(".*/(article|post|news|detail|view)/.*|.*\\.html$|.*/\\d+\\.html$");
        boolean urlLooksLikeList = url.matches(".*/(list|index|category|tag|archive)/.*|.*/page/\\d+.*");
        
        // URL模式是强信号：如果URL明确包含 /page/，优先判断为列表页
        if (url.contains("/page/") && url.matches(".*/page/\\d+.*"))
        {
            log.debug("URL {} 包含 /page/，优先判断为列表页", url);
            return PageType.LIST;
        }
        
        // URL模式是强信号：如果URL包含 /category/ 且路径段 <= 2（只有分类1和分类2，没有文章标题），优先判断为列表页
        // 例如：/category/wanghong/fuliji 是列表页（2个分类段）
        // 但 /category/wanghong/twitter/某个标题/ 可能是详情页（路径段 >= 3，有文章标题）
        if (url.contains("/category/"))
        {
            try
            {
                URI uri = new URI(url);
                String path = uri.getPath();
                if (path != null)
                {
                    String[] pathSegments = path.split("/");
                    int categorySegments = 0; // 统计 category 后面的非空路径段数量
                    boolean foundCategory = false;
                    for (String segment : pathSegments)
                    {
                        if ("category".equals(segment))
                        {
                            foundCategory = true;
                            continue;
                        }
                        if (foundCategory && segment != null && !segment.isEmpty())
                        {
                            categorySegments++;
                        }
                    }
                    // 如果 category 后面只有 <= 2 个路径段，判断为列表页
                    // 例如：/category/wanghong/fuliji 有2个路径段（wanghong, fuliji），是列表页
                    if (categorySegments <= 2)
                    {
                        log.debug("URL {} 包含 /category/ 且路径段 <= 2（{}个），优先判断为列表页", url, categorySegments);
                        return PageType.LIST;
                    }
                }
            }
            catch (Exception e)
            {
                log.debug("解析URL路径失败: {}", url, e);
            }
        }
        
        int detailScore = 0;
        int listScore = 0;
        
        // 详情页评分
        if (hasSingleArticleContent) detailScore += 4; // 单个主要内容区域是强信号
        else if (hasArticleContent) detailScore += 2; // 多个 article 可能是列表页
        if (hasSingleTitle) detailScore += 2;
        if (hasArticleMeta) detailScore += 2;
        // URL模式是强信号：如果URL看起来像详情页（.html结尾），且只有一个主要内容区域，优先判断为详情页
        if (urlLooksLikeDetail)
        {
            if (hasSingleArticleContent)
            {
                detailScore += 4; // 强信号：URL像详情页 + 单个主要内容区域
            }
            else
            {
                detailScore += 2; // 中等信号：URL像详情页
            }
        }
        
        // 列表页评分
        if (hasListContainer) listScore += 3;
        if (hasMultipleCards) listScore += 4; // 多个卡片是列表页的强信号
        if (hasPagination) listScore += 3; // 分页是列表页的强信号
        if (hasMultipleLinks && !hasSingleArticleContent) listScore += 2;
        if (urlLooksLikeList) listScore += 3; // URL 模式是强信号
        
        // 特殊处理：如果URL看起来像详情页（.html结尾），但被判断为列表页，需要更严格的检查
        // 避免将详情页误判为列表页导致递归提取
        if (urlLooksLikeDetail && listScore >= 5 && detailScore < 5)
        {
            // URL像详情页但被判断为列表页，检查是否真的有多个独立的文章卡片
            // 如果只有一个主要内容区域，即使有多个卡片选择器匹配，也应该优先判断为详情页
            if (hasSingleArticleContent)
            {
                log.debug("URL {} 看起来像详情页且只有一个主要内容区域，强制判断为详情页", url);
                return PageType.DETAIL;
            }
        }
        
        if (detailScore >= 5 && listScore < 3)
        {
            return PageType.DETAIL;
        }
        else if (listScore >= 5 && detailScore < 3)
        {
            return PageType.LIST;
        }
        else if (detailScore >= 3 && listScore >= 3)
        {
            // 兼容：某些分类/归档列表页会同时出现 h1/time 等，导致被判定为 MIXED。
            // 只要列表信号足够强（多卡片 + 分页），优先当作 LIST 处理。
            if (!urlLooksLikeDetail && hasMultipleCards && hasPagination && listScore >= 6)
            {
                return PageType.LIST;
            }
            return PageType.MIXED;
        }
        else
        {
            // 默认根据URL判断
            return urlLooksLikeDetail ? PageType.DETAIL : PageType.LIST;
        }
    }
    
    /**
     * 从列表页提取文章链接
     * 支持两种布局：
     * 1. 传统列表布局（一行一行）
     * 2. 图片卡片网格布局（如 jrants.com）
     */
    private void extractArticleLinks(CrawlerTaskEntity task, String url, Document doc,
                                    Queue<UrlInfo> urlQueue, Set<String> visitedUrls,
                                    String baseUrl, String scopeType, int maxDepth,
                                    AtomicInteger totalUrls, int maxUrls)
    {
        Set<String> foundLinks = new HashSet<>();
        
        // 策略1：尝试识别"卡片容器"布局（图片散落的网格布局）
        // 优先使用精确的选择器，避免匹配到太多无关元素
        String[] cardContainerSelectors = {
            "article.post",           // WordPress 标准结构
            "article.entry",          // WordPress 标准结构
            "article[class*='post']", // 包含 post 的 article
            "article[class*='entry']",// 包含 entry 的 article
            ".post",                  // .post 类（需要手动检查是否包含图片）
            ".entry",                 // .entry 类
            ".article-item",           // .article-item 类
            ".card",                  // .card 类
            ".post-item",             // .post-item 类
            ".entry-item"             // .entry-item 类
        };
        
        boolean foundCards = false;
        for (String containerSelector : cardContainerSelectors)
        {
            try
            {
                Elements cards = doc.select(containerSelector);
                // 过滤：只保留包含图片的卡片（避免匹配到 .post-meta 等无关元素）
                List<Element> validCards = new ArrayList<>();
                for (Element card : cards)
                {
                    // 检查卡片是否包含图片（至少一张）
                    Elements imgs = card.select("img");
                    if (!imgs.isEmpty())
                    {
                        validCards.add(card);
                    }
                }
                
                // 至少找到2个有效卡片才认为是卡片布局（避免误判）
                if (validCards.size() >= 2)
                {
                    foundCards = true;
                    log.debug("检测到卡片布局，使用选择器: {}, 找到 {} 个有效卡片", containerSelector, validCards.size());
                    
                    for (Element card : validCards)
                    {
                        // 从每个卡片中提取链接
                        // 优先查找卡片内的主要链接（通常是封面图或标题的链接）
                        Element linkElement = null;
                        
                        // 1. 优先查找卡片内最大的图片链接（通常是封面）
                        Elements allLinks = card.select("a[href]");
                        Element largestImgLink = null;
                        int maxSize = 0;
                        
                        for (Element link : allLinks)
                        {
                            // 检查链接内是否有图片
                            Element img = link.selectFirst("img");
                            if (img != null)
                            {
                                // 计算图片尺寸（用于选择最大的图片链接）
                                String width = img.attr("width");
                                String height = img.attr("height");
                                int size = 0;
                                if (width != null && !width.isEmpty() && height != null && !height.isEmpty())
                                {
                                    try
                                    {
                                        size = Integer.parseInt(width) * Integer.parseInt(height);
                                    }
                                    catch (NumberFormatException e)
                                    {
                                        // 如果无法解析尺寸，使用默认值
                                        size = 1000; // 默认值，确保会被选中
                                    }
                                }
                                else
                                {
                                    // 如果没有尺寸信息，使用默认值
                                    size = 1000;
                                }
                                
                                if (size > maxSize)
                                {
                                    maxSize = size;
                                    largestImgLink = link;
                                }
                            }
                        }
                        
                        if (largestImgLink != null)
                        {
                            linkElement = largestImgLink;
                        }
                        
                        // 2. 如果没有找到图片链接，查找标题链接
                        if (linkElement == null)
                        {
                            linkElement = card.selectFirst("h2 a, h3 a, h4 a, .title a, .entry-title a, .post-title a");
                        }
                        
                        // 3. 如果还没有，查找卡片内的第一个主要链接（排除导航、侧边栏等）
                        if (linkElement == null)
                        {
                            // 排除导航和侧边栏链接
                            Elements mainLinks = card.select("a[href]");
                            for (Element link : mainLinks)
                            {
                                String href = link.attr("abs:href");
                                // 排除明显的非文章链接
                                if (href != null && !href.contains("/tag/") && !href.contains("/author/") 
                                    && !href.contains("/category/") && !href.contains("/page/"))
                                {
                                    linkElement = link;
                                    break;
                                }
                            }
                        }
                        
                        // 4. 如果卡片本身是链接
                        if (linkElement == null && card.tagName().equals("a"))
                        {
                            linkElement = card;
                        }
                        
                        if (linkElement != null)
                        {
                            String href = linkElement.attr("abs:href");
                            if (href != null && !href.isEmpty() && !href.equals(url))
                            {
                                // 对于卡片布局，放宽 isArticleLink 的检查
                                if (isArticleLinkForCardLayout(href, url))
                                {
                                    foundLinks.add(href);
                                }
                            }
                        }
                    }
                    
                    // 如果找到卡片布局且提取到链接，优先使用这种方式
                    if (foundLinks.size() > 0)
                    {
                        log.info("使用卡片布局策略，从 {} 提取到 {} 个文章链接", url, foundLinks.size());
                        break;
                    }
                }
            }
            catch (Exception e)
            {
                // 忽略错误继续尝试下一个选择器
                log.debug("选择器 {} 执行失败: {}", containerSelector, e.getMessage());
            }
        }
        
        // 策略2：如果没有找到卡片布局或提取失败，使用传统的链接选择器（兼容传统列表布局）
        if (!foundCards || foundLinks.isEmpty())
        {
            String[] articleLinkSelectors = {
                "a[href*='/article/']",
                "a[href*='/post/']",
                "a[href*='/news/']",
                "a[href*='/detail/']",
                "a[href*='.html']",
                ".article-list a",
                ".post-list a",
                ".news-list a",
                ".list-item a",
                "article a",
                ".title a",
                "h2 a, h3 a, h4 a" // 标题链接
            };
            
            for (String selector : articleLinkSelectors)
            {
                Elements links = doc.select(selector);
                for (Element link : links)
                {
                    String href = link.attr("abs:href");
                    if (href != null && !href.isEmpty() && !href.equals(url))
                    {
                        if (isArticleLink(href))
                        {
                            foundLinks.add(href);
                        }
                    }
                }
            }
        }
        
        // 添加到队列
        // 限制：从列表页提取的文章数量，避免一次性提取过多
        // 默认限制为20个，如果用户设置了listMaxPages，可以根据页数调整
        int maxArticlesPerPage = 20; // 每页最多提取20个文章链接
        int addedCount = 0;
        for (String href : foundLinks)
        {
            if (addedCount >= maxArticlesPerPage || totalUrls.get() >= maxUrls)
            {
                break;
            }
            
            String normalizedUrl = normalizeUrl(href);
            if (!visitedUrls.contains(normalizedUrl))
            {
                // 检查站点范围
                if ("SITE".equals(scopeType))
                {
                    String linkBaseUrl = extractBaseUrl(href);
                    if (!baseUrl.equals(linkBaseUrl))
                    {
                        continue;
                    }
                }
                
                // 列表页提取的文章链接深度设为1（而不是0），这样如果被误判为列表页，不会再次提取链接
                urlQueue.offer(new UrlInfo(href, 1));
                visitedUrls.add(normalizedUrl);
                totalUrls.incrementAndGet();
                addedCount++;
            }
        }
        
        if (foundLinks.size() > maxArticlesPerPage)
        {
            log.info("列表页 {} 共找到 {} 个文章链接，但只提取前 {} 个（防止提取过多）", 
                    url, foundLinks.size(), maxArticlesPerPage);
        }
        
        log.info("从列表页 {} 提取到 {} 个文章链接（使用{}策略）", 
                url, foundLinks.size(), foundCards ? "卡片容器" : "传统链接");
    }
    
    /**
     * 判断是否为文章链接
     */
    private boolean isArticleLink(String url)
    {
        if (url == null || url.isEmpty())
        {
            return false;
        }
        
        // 排除的URL模式（列表页、功能页等）
        String[] excludePatterns = {
            "/tag/", "/author/", "/search", "/login", "/register",
            "/about", "/contact", "/privacy", "/terms", "/#", "javascript:", "mailto:",
            "/page/", "?page=", "&page=", // 分页链接
            "/feed", "/rss", "/sitemap" // Feed和站点地图
        };
        
        for (String pattern : excludePatterns)
        {
            if (url.contains(pattern))
            {
                return false;
            }
        }
        
        // 特殊处理：/category/ 开头的URL
        // 如果URL是 /category/xxx/yyy/ 格式，且不是分页链接，可能是文章链接
        // 例如：/category/wanghong/twitter/某个标题/ 是文章链接
        // 但 /category/wanghong/twitter/page/2 是列表页链接
        if (url.contains("/category/"))
        {
            // 排除分页链接
            if (url.matches(".*/category/[^/]+/[^/]+/page/\\d+.*") || 
                url.matches(".*/category/[^/]+/[^/]+\\?page=\\d+.*"))
            {
                return false;
            }
            // 如果 /category/ 后面有多于2个路径段，可能是文章链接
            // 例如：/category/wanghong/twitter/某个标题/ 有4个路径段
            String path = url.replaceFirst("https?://[^/]+", "");
            String[] pathSegments = path.split("/");
            int nonEmptySegments = 0;
            for (String seg : pathSegments)
            {
                if (seg != null && !seg.isEmpty() && !seg.contains("?"))
                {
                    nonEmptySegments++;
                }
            }
            // 如果路径段 >= 4（category + 分类1 + 分类2 + 文章标题），可能是文章链接
            if (nonEmptySegments >= 4)
            {
                return true;
            }
            // 否则是分类列表页
            return false;
        }
        
        // 文章URL常见模式
        String[] articlePatterns = {
            "/article/", "/post/", "/news/", "/detail/", "/view/", ".html", "/\\d+"
        };
        
        for (String pattern : articlePatterns)
        {
            if (url.contains(pattern))
            {
                return true;
            }
        }
        
        // 如果URL看起来像是一个具体的页面（不是列表页），也可能是文章链接
        // 例如：/某个标题/ 或 /某个标题.html
        // 但需要排除明显的列表页模式
        if (!url.contains("/list") && !url.contains("/index") && 
            !url.contains("/archive") && !url.matches(".*/page/\\d+.*"))
        {
            // 如果URL路径段数 >= 2（不包括域名），可能是文章链接
            String path = url.replaceFirst("https?://[^/]+", "");
            String[] pathSegments = path.split("/");
            int nonEmptySegments = 0;
            for (String seg : pathSegments)
            {
                if (seg != null && !seg.isEmpty() && !seg.contains("?") && !seg.contains("#"))
                {
                    nonEmptySegments++;
                }
            }
            // 如果路径段 >= 2，且不是明显的列表页，可能是文章链接
            if (nonEmptySegments >= 2)
            {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 判断是否为文章链接（针对卡片布局，更宽松的检查）
     * 用于图片散落的网格布局（如 jrants.com），放宽对 WordPress permalink 的检查
     */
    private boolean isArticleLinkForCardLayout(String url, String currentPageUrl)
    {
        if (url == null || url.isEmpty())
        {
            return false;
        }
        
        // 排除明显的非文章链接
        String[] excludePatterns = {
            "/tag/", "/author/", "/search", "/login", "/register",
            "/about", "/contact", "/privacy", "/terms", "/#", "javascript:", "mailto:",
            "/feed", "/rss", "/sitemap", "/wp-admin", "/wp-content", "/wp-includes"
        };
        
        for (String pattern : excludePatterns)
        {
            if (url.contains(pattern))
            {
                return false;
            }
        }
        
        // 排除分页链接（更严格的检查）
        if (url.matches(".*/page/\\d+.*") || url.matches(".*[?&]page=\\d+.*"))
        {
            return false;
        }
        
        // 特殊处理：/category/ 开头的URL（WordPress permalink 结构）
        // jrants.com 的文章链接格式：/category/wanghong/twitter/某个标题/
        if (url.contains("/category/"))
        {
            // 排除分页链接
            if (url.matches(".*/category/[^/]+/[^/]+/page/\\d+.*") || 
                url.matches(".*/category/[^/]+/[^/]+\\?page=\\d+.*"))
            {
                return false;
            }
            
            // 计算路径段数
            String path = url.replaceFirst("https?://[^/]+", "").split("\\?")[0]; // 去掉查询参数
            String[] pathSegments = path.split("/");
            int nonEmptySegments = 0;
            for (String seg : pathSegments)
            {
                if (seg != null && !seg.isEmpty())
                {
                    nonEmptySegments++;
                }
            }
            
            // WordPress permalink: /category/分类1/分类2/文章标题/
            // 如果路径段 >= 4（category + 分类1 + 分类2 + 文章标题），很可能是文章链接
            if (nonEmptySegments >= 4)
            {
                return true;
            }
            
            // 如果路径段 == 3（category + 分类1 + 分类2），且当前页面也是 category 列表页，则这是列表页链接
            if (nonEmptySegments == 3 && currentPageUrl != null && currentPageUrl.contains("/category/"))
            {
                return false;
            }
        }
        
        // 文章URL常见模式
        String[] articlePatterns = {
            "/article/", "/post/", "/news/", "/detail/", "/view/", ".html"
        };
        
        for (String pattern : articlePatterns)
        {
            if (url.contains(pattern))
            {
                return true;
            }
        }
        
        // 对于卡片布局，如果URL路径段数 >= 2，且不是明显的列表页，可能是文章链接
        String path = url.replaceFirst("https?://[^/]+", "").split("\\?")[0];
        String[] pathSegments = path.split("/");
        int nonEmptySegments = 0;
        for (String seg : pathSegments)
        {
            if (seg != null && !seg.isEmpty() && !seg.contains("#"))
            {
                nonEmptySegments++;
            }
        }
        
        // 排除明显的列表页模式
        if (url.contains("/list") || url.contains("/index") || url.contains("/archive"))
        {
            return false;
        }
        
        // 如果路径段 >= 2，且不是当前列表页本身，可能是文章链接
        if (nonEmptySegments >= 2 && !url.equals(currentPageUrl))
        {
            return true;
        }
        
        return false;
    }
    
    /**
     * 提取所有链接（兜底逻辑）
     */
    private void extractAllLinks(Document doc, Queue<UrlInfo> urlQueue, Set<String> visitedUrls,
                                String baseUrl, String scopeType, int currentDepth, int maxDepth,
                                AtomicInteger totalUrls, int maxUrls)
    {
        Elements links = doc.select("a[href]");
        int addedCount = 0;
        for (Element link : links)
        {
            if (addedCount >= 50 || totalUrls.get() >= maxUrls || currentDepth >= maxDepth)
            {
                break;
            }
            
            String href = link.attr("abs:href");
            if (href != null && !href.isEmpty())
            {
                String normalizedUrl = normalizeUrl(href);
                if (!visitedUrls.contains(normalizedUrl))
                {
                    // 检查站点范围
                    if ("SITE".equals(scopeType))
                    {
                        String linkBaseUrl = extractBaseUrl(href);
                        if (!baseUrl.equals(linkBaseUrl))
                        {
                            continue;
                        }
                    }
                    
                    urlQueue.offer(new UrlInfo(href, currentDepth + 1));
                    visitedUrls.add(normalizedUrl);
                    totalUrls.incrementAndGet();
                    addedCount++;
                }
            }
        }
    }

    private void enqueueNextListPageIfNeeded(CrawlerTaskEntity task,
                                             UrlInfo currentUrlInfo,
                                             Document doc,
                                             Queue<UrlInfo> urlQueue,
                                             Set<String> visitedUrls,
                                             String baseUrl,
                                             String scopeType,
                                             AtomicInteger totalUrls,
                                             int maxUrls)
    {
        Integer listMaxPages = task.getListMaxPages();
        if (listMaxPages == null || listMaxPages < 2)
        {
            // 1页不需要翻页
            return;
        }

        int processed = listPagesProcessed.merge(task.getId(), 1, Integer::sum);
        if (processed >= listMaxPages)
        {
            return;
        }

        String nextUrl = extractNextPageUrl(doc);
        if (nextUrl == null || nextUrl.isEmpty())
        {
            return;
        }

        String normalizedUrl = normalizeUrl(nextUrl);
        if (visitedUrls.contains(normalizedUrl))
        {
            return;
        }

        if ("SITE".equals(scopeType))
        {
            String linkBaseUrl = extractBaseUrl(nextUrl);
            if (!baseUrl.equals(linkBaseUrl))
            {
                return;
            }
        }

        if (totalUrls.get() >= maxUrls)
        {
            return;
        }

        visitedUrls.add(normalizedUrl);
        totalUrls.incrementAndGet();
        urlQueue.offer(new UrlInfo(nextUrl, Math.max(0, currentUrlInfo.depth + 1)));
        log.info("列表页翻页：已处理 {}/{}，加入下一页: {}", processed, listMaxPages, nextUrl);
    }

    private String extractNextPageUrl(Document doc)
    {
        // 1. 直接的 next/rel=next 等入口
        Element next = doc.selectFirst(
                "a[rel=next], " +
                "a#next, " +
                ".nav-links a.next, " +
                ".pagination a.next, " +
                ".page-numbers.next, " +
                // ouxpa.com 等站点：Bootstrap 风格分页
                ".page-nav a.page-next, " +
                ".pagination a.page-next, " +
                ".pagination a.page-link.page-next");
        if (next == null)
        {
            // 2. 文本為“下一页/Next/»”等的連結（常見於 WordPress、國內站）
            next = doc.selectFirst(
                    ".pages a:matchesOwn(下一页|下一頁|Next|›|»), " +
                    ".pagination a:matchesOwn(下一页|下一頁|Next|›|»), " +
                    ".page-numbers a:matchesOwn(下一页|下一頁|Next|›|»), " +
                    // ouxpa.com：a.page-link.page-next 文本包含“下一页”
                    ".page-nav a:matchesOwn(下一页|下一頁|Next|›|»)");
        }
        if (next != null)
        {
            String href = next.attr("abs:href");
            if (href != null && !href.isEmpty() && !href.startsWith("javascript:"))
            {
                return href;
            }
        }
        return "";
    }
    
    /**
     * 提取文章内容
     * @return 文章ID，如果提取失败返回null
     */
    private Long extractArticle(CrawlerTaskEntity task, String url, Document doc, AtomicInteger successCount)
    {
        try
        {
            // 简单的文章提取逻辑（可以根据实际需求优化）
            String title = doc.title();
            if (title == null || title.isEmpty())
            {
                Elements titleElements = doc.select("h1, h2, .title, .article-title");
                if (!titleElements.isEmpty())
                {
                    title = titleElements.first().text();
                }
            }
            
            // 提取正文
            Elements contentElements = doc.select("article, .content, .article-content, .post-content, #content");
            String content = "";
            if (!contentElements.isEmpty())
            {
                content = contentElements.first().text();
            }
            else
            {
                // 如果没有找到特定容器，尝试提取body中的文本
                content = doc.body().text();
            }
            
            // 提取发布时间
            Date publishTime = null;
            Elements timeElements = doc.select("time, .publish-time, .date, .post-date");
            if (!timeElements.isEmpty())
            {
                String timeStr = timeElements.first().attr("datetime");
                if (timeStr == null || timeStr.isEmpty())
                {
                    timeStr = timeElements.first().text();
                }
                // 简单的时间解析（可以根据实际需求优化）
            }
            
            // 提取作者
            String author = "";
            Elements authorElements = doc.select(".author, .writer, .post-author");
            if (!authorElements.isEmpty())
            {
                author = authorElements.first().text();
            }
            
            // 保存文章
            if (title != null && !title.isEmpty() && content != null && !content.isEmpty())
            {
                CrawlerArticleEntity article = new CrawlerArticleEntity();
                article.setTaskId(task.getId());
                article.setTitle(title);
                article.setContent(content);
                article.setUrl(url);
                article.setUrlHash(DigestUtil.md5Hex(url)); // 计算URL的MD5哈希值
                article.setAuthor(author);
                article.setSourceSite(extractBaseUrl(url));
                article.setPublishTime(publishTime);
                article.setCreateTime(new Date());
                // 继承任务的创建人和部门信息（数据权限）
                article.setCreateBy(task.getCreateBy());
                article.setDeptId(task.getDeptId());
                articleService.save(article);
                successCount.incrementAndGet();
                return article.getId();
            }
        }
        catch (Exception e)
        {
            log.error("提取文章失败: {}", url, e);
        }
        return null;
    }
    
    /**
     * 提取图片
     */
    private void extractImages(CrawlerTaskEntity task, String url, Document doc, AtomicInteger successCount)
    {
        try
        {
            Elements imgElements = doc.select("img[src]");
            for (Element img : imgElements)
            {
                String imgSrc = img.attr("abs:src");
                if (imgSrc != null && !imgSrc.isEmpty())
                {
                    CrawlerImageEntity image = new CrawlerImageEntity();
                    image.setTaskId(task.getId());
                    image.setUrl(imgSrc);
                    image.setUrlHash(DigestUtil.md5Hex(imgSrc)); // 计算URL的MD5哈希值
                    image.setDownloadStatus("PENDING");
                    image.setFormat(extractFileExtension(imgSrc));
                    image.setCreateTime(new Date());
                    imageService.save(image);

                    // 纯图片任务（无文章ID），如果开启下载，则使用任务级目录
                    if (task.getDownloadImages() != null && task.getDownloadImages() == 1)
                    {
                        downloadImageToLocal(task, null, image, imgSrc, url);
                    }

                    successCount.incrementAndGet();
                }
            }
        }
        catch (Exception e)
        {
            log.error("提取图片失败: {}", url, e);
        }
    }
    
    /**
     * 解析起始URL列表
     */
    @SuppressWarnings("unchecked")
    private List<String> parseStartUrls(String startUrlsJson)
    {
        if (startUrlsJson == null || startUrlsJson.isEmpty())
        {
            return Collections.emptyList();
        }
        try
        {
            return objectMapper.readValue(startUrlsJson, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        }
        catch (Exception e)
        {
            log.error("解析起始URL失败", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * 标准化URL
     */
    private String normalizeUrl(String url)
    {
        try
        {
            URI uri = new URI(url);
            return uri.normalize().toString();
        }
        catch (URISyntaxException e)
        {
            return url;
        }
    }
    
    /**
     * 提取基础URL
     */
    private String extractBaseUrl(String url)
    {
        try
        {
            URI uri = new URI(url);
            return uri.getScheme() + "://" + uri.getHost();
        }
        catch (URISyntaxException e)
        {
            return url;
        }
    }
    
    /**
     * 获取User-Agent（支持轮换）
     */
    private String getUserAgent(CrawlerTaskEntity task, int retry)
    {
        // 如果配置了自定义User-Agent且不轮换，直接返回
        if (task.getUserAgent() != null && !task.getUserAgent().isEmpty())
        {
            if (task.getRotateUserAgent() == null || task.getRotateUserAgent() == 0)
            {
                return task.getUserAgent();
            }
        }
        
        // User-Agent池（常见浏览器User-Agent）
        String[] userAgents = {
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.1 Safari/605.1.15",
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
        };
        
        // 如果配置了轮换，根据重试次数或随机选择
        if (task.getRotateUserAgent() != null && task.getRotateUserAgent() == 1)
        {
            int index = (retry + (int)(System.currentTimeMillis() % userAgents.length)) % userAgents.length;
            return userAgents[index];
        }
        
        return userAgents[0]; // 默认返回第一个
    }
    
    /**
     * 添加自定义请求头
     */
    private void addCustomHeaders(org.jsoup.Connection connection, CrawlerTaskEntity task)
    {
        if (task.getHeaders() == null || task.getHeaders().isEmpty())
        {
            // 如果没有配置自定义请求头，添加默认的浏览器请求头
            // 注意：不设置 Accept-Encoding，让 Jsoup 自动处理（避免解压问题）
            connection.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
            connection.header("Accept-Language", "zh-CN,zh;q=0.9,en-US;q=0.8,en;q=0.7");
            connection.header("Connection", "keep-alive");
            connection.header("Upgrade-Insecure-Requests", "1");
            connection.header("Sec-Fetch-Dest", "document");
            connection.header("Sec-Fetch-Mode", "navigate");
            connection.header("Sec-Fetch-Site", "same-origin");
            connection.header("Sec-Fetch-User", "?1");
            connection.header("Cache-Control", "max-age=0");
            connection.header("DNT", "1"); // Do Not Track
            return;
        }
        
        try
        {
            // 解析自定义请求头（JSON格式：{"Header-Name": "value"}）
            Map<String, String> headers = objectMapper.readValue(task.getHeaders(), 
                    objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
            for (Map.Entry<String, String> entry : headers.entrySet())
            {
                connection.header(entry.getKey(), entry.getValue());
            }
        }
        catch (Exception e)
        {
            log.warn("解析自定义请求头失败，使用默认请求头: {}", e.getMessage());
            // 解析失败时使用默认请求头
            connection.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
            connection.header("Accept-Language", "zh-CN,zh;q=0.9");
        }
    }
    
    /**
     * 添加Cookie
     */
    private void addCookies(org.jsoup.Connection connection, CrawlerTaskEntity task)
    {
        if (task.getCookies() == null || task.getCookies().isEmpty())
        {
            return;
        }
        
        try
        {
            // 解析Cookie（JSON格式：{"name": "value"} 或字符串格式："name1=value1; name2=value2"）
            String cookies = task.getCookies().trim();
            if (cookies.startsWith("{"))
            {
                // JSON格式
                Map<String, String> cookieMap = objectMapper.readValue(cookies, 
                        objectMapper.getTypeFactory().constructMapType(Map.class, String.class, String.class));
                for (Map.Entry<String, String> entry : cookieMap.entrySet())
                {
                    connection.cookie(entry.getKey(), entry.getValue());
                }
            }
            else
            {
                // 字符串格式：name1=value1; name2=value2
                String[] cookiePairs = cookies.split(";");
                for (String pair : cookiePairs)
                {
                    String[] kv = pair.trim().split("=", 2);
                    if (kv.length == 2)
                    {
                        connection.cookie(kv[0].trim(), kv[1].trim());
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.warn("解析Cookie失败: {}", e.getMessage());
        }
    }
    
    /**
     * 从详情页提取图片并关联到文章
     */
    private void extractImagesFromDetailPage(CrawlerTaskEntity task, Long articleId, String url, 
                                            Document doc, AtomicInteger successCount)
    {
        try
        {
            log.debug("开始从详情页 {} 提取图片，articleId: {}", url, articleId);
            
            Elements imgElements;
            
            // 优先使用用户配置的选择器
            String contentSelector = task.getContentSelector();
            String imageSelector = task.getImageSelector();
            String excludeSelector = task.getExcludeSelector();
            
            // 调试日志：输出配置信息
            log.info("任务 {} 图片提取配置 - contentSelector: [{}], imageSelector: [{}], excludeSelector: [{}]", 
                task.getId(), contentSelector, imageSelector, excludeSelector);
            
            // 如果用户配置了图片选择器，优先使用（即使没有配置正文容器选择器）
            if (imageSelector != null && !imageSelector.trim().isEmpty())
            {
                // 如果用户配置的选择器不包含 img 标签，自动添加 img 标签
                // 例如：.entry-content -> .entry-content img
                String actualImageSelector = imageSelector.trim();
                String lowerSelector = actualImageSelector.toLowerCase();
                boolean needAddImg = !lowerSelector.contains("img") && !lowerSelector.contains("image");
                
                if (needAddImg)
                {
                    // 如果选择器以空格结尾，直接添加 img，否则添加空格和 img
                    if (actualImageSelector.endsWith(" "))
                    {
                        actualImageSelector = actualImageSelector + "img";
                    }
                    else
                    {
                        actualImageSelector = actualImageSelector + " img";
                    }
                    log.info("详情页 {} 用户配置的图片选择器不包含 img 标签，自动添加: {} -> {}", url, imageSelector, actualImageSelector);
                }
                
                if (contentSelector != null && !contentSelector.trim().isEmpty())
                {
                    // 如果同时配置了正文容器：严格在“最佳正文容器”中查找（不再回退到整页）
                    Element contentRoot = selectBestContentRoot(doc, contentSelector, actualImageSelector);
                    if (contentRoot != null)
                    {
                        log.info("详情页 {} 使用用户配置的正文容器选择器: {}，找到容器", url, contentSelector);
                        imgElements = contentRoot.select(actualImageSelector);
                        log.info("详情页 {} 在正文容器中使用用户配置的图片选择器: {}，找到 {} 个图片元素", url, actualImageSelector, imgElements.size());
                    }
                    else
                    {
                        // 关键改动：用户显式配置了正文容器选择器 => 不回退到整页，避免抓到正文外图片
                        log.warn("详情页 {} 使用用户配置的正文容器选择器: {}，但未找到任何匹配容器，已跳过整页回退（避免误抓正文外图片）", url, contentSelector);
                        imgElements = new Elements();
                    }
                }
                else
                {
                    // 只配置了图片选择器，先从整个文档中选择
                    Elements selectedElements = doc.select(actualImageSelector);
                    
                    // 检查选择到的元素是否是 img 元素，如果不是，尝试在这些元素中查找 img
                    imgElements = new Elements();
                    for (Element element : selectedElements)
                    {
                        if ("img".equalsIgnoreCase(element.tagName()))
                        {
                            // 是 img 元素，直接添加
                            imgElements.add(element);
                        }
                        else
                        {
                            // 不是 img 元素，可能是容器，在容器中查找 img
                            Elements imgsInContainer = element.select("img");
                            imgElements.addAll(imgsInContainer);
                            if (!imgsInContainer.isEmpty())
                            {
                                log.debug("详情页 {} 选择器 {} 选择到容器元素 {}，在容器中找到 {} 个 img 元素", 
                                    url, actualImageSelector, element.tagName(), imgsInContainer.size());
                            }
                        }
                    }
                    
                    log.info("详情页 {} 从整个文档中使用用户配置的图片选择器: {}，找到 {} 个图片元素", url, actualImageSelector, imgElements.size());
                }
                
                // 如果配置了排除选择器，应用排除选择器
                if (excludeSelector != null && !excludeSelector.trim().isEmpty() && imgElements != null && !imgElements.isEmpty())
                {
                    int originalCount = imgElements.size();
                    Elements filteredElements = new Elements();
                    // 支持英文逗号、中文逗号、分号分隔多个选择器
                    String[] excludeSelectors = excludeSelector.split("[,，;]+");
                    
                    // 先找到所有匹配排除选择器的元素
                    Elements excludeElements = new Elements();
                    for (String selector : excludeSelectors)
                    {
                        selector = selector.trim();
                        if (selector.isEmpty()) continue;
                        
                        try
                        {
                            Elements matched = doc.select(selector);
                            excludeElements.addAll(matched);
                            if (!matched.isEmpty())
                            {
                                log.debug("详情页 {} 排除选择器 {} 匹配到 {} 个元素", url, selector, matched.size());
                            }
                        }
                        catch (Exception e)
                        {
                            log.warn("排除选择器 {} 查询时出错: {}", selector, e.getMessage());
                        }
                    }
                    
                    // 如果找到了排除元素，检查每个图片是否在这些元素内
                    if (!excludeElements.isEmpty())
                    {
                        log.info("详情页 {} 排除选择器共匹配到 {} 个元素，开始过滤图片", url, excludeElements.size());
                    }
                    
                    for (Element img : imgElements)
                    {
                        boolean shouldExclude = false;
                        String imgSrc = img.attr("src");
                        if (imgSrc == null || imgSrc.isEmpty())
                        {
                            imgSrc = img.attr("data-src");
                        }
                        
                        // 检查图片本身或任何祖先元素是否匹配排除选择器
                        Element current = img;
                        int depth = 0;
                        while (current != null && depth < 20)
                        {
                            // 检查当前元素是否在排除元素列表中
                            for (Element excludeElement : excludeElements)
                            {
                                if (excludeElement.equals(current))
                                {
                                    shouldExclude = true;
                                    log.info("图片 {} 位于排除选择器匹配的元素内（深度: {}，元素: {}）", 
                                        imgSrc, depth, excludeElement.tagName() + (excludeElement.className().isEmpty() ? "" : "." + excludeElement.className()));
                                    break;
                                }
                            }
                            
                            if (shouldExclude)
                            {
                                break;
                            }
                            
                            // 也检查当前元素是否匹配排除选择器本身
                            for (String selector : excludeSelectors)
                            {
                                selector = selector.trim();
                                if (selector.isEmpty()) continue;
                                
                                try
                                {
                                    if (current.is(selector))
                                    {
                                        shouldExclude = true;
                                        log.info("图片 {} 的祖先元素（深度: {}）匹配排除选择器: {}", imgSrc, depth, selector);
                                        break;
                                    }
                                }
                                catch (Exception e)
                                {
                                    // 忽略选择器错误
                                }
                            }
                            
                            if (shouldExclude)
                            {
                                break;
                            }
                            
                            current = current.parent();
                            depth++;
                        }
                        
                        if (!shouldExclude)
                        {
                            filteredElements.add(img);
                        }
                        else
                        {
                            log.info("图片 {} 被排除选择器过滤", imgSrc);
                        }
                    }
                    imgElements = filteredElements;
                    log.info("详情页 {} 应用排除选择器: {}，过滤后剩余 {} 个图片元素（原始: {} 个，过滤: {} 个）", 
                        url, excludeSelector, imgElements.size(), originalCount, originalCount - imgElements.size());
                }
            }
            else if (contentSelector != null && !contentSelector.trim().isEmpty())
            {
                // 用户只配置了正文容器选择器，没有配置图片选择器
                Element contentRoot = selectBestContentRoot(doc, contentSelector, "img");
                if (contentRoot != null)
                {
                    log.info("详情页 {} 使用用户配置的正文容器选择器: {}，找到容器", url, contentSelector);
                    
                    // 如果用户配置了排除选择器，应用排除选择器
                    if (excludeSelector != null && !excludeSelector.trim().isEmpty())
                    {
                        // 先获取所有图片，然后手动过滤
                        Elements allImgs = contentRoot.select("img");
                        Elements filteredElements = new Elements();
                        // 支持英文逗号、中文逗号、分号分隔多个选择器
                        String[] excludeSelectors = excludeSelector.split("[,，;]+");
                        
                        // 先找到所有匹配排除选择器的元素
                        Elements excludeElements = new Elements();
                        for (String selector : excludeSelectors)
                        {
                            selector = selector.trim();
                            if (selector.isEmpty()) continue;
                            
                            try
                            {
                                Elements matched = doc.select(selector);
                                excludeElements.addAll(matched);
                                if (!matched.isEmpty())
                                {
                                    log.debug("详情页 {} 排除选择器 {} 匹配到 {} 个元素", url, selector, matched.size());
                                }
                            }
                            catch (Exception e)
                            {
                                log.warn("排除选择器 {} 查询时出错: {}", selector, e.getMessage());
                            }
                        }
                        
                        // 如果找到了排除元素，检查每个图片是否在这些元素内
                        if (!excludeElements.isEmpty())
                        {
                            log.info("详情页 {} 排除选择器共匹配到 {} 个元素，开始过滤图片", url, excludeElements.size());
                        }
                        
                        for (Element img : allImgs)
                        {
                            boolean shouldExclude = false;
                            String imgSrc = img.attr("src");
                            if (imgSrc == null || imgSrc.isEmpty())
                            {
                                imgSrc = img.attr("data-src");
                            }
                            
                            // 检查图片本身或任何祖先元素是否匹配排除选择器
                            Element current = img;
                            int depth = 0;
                            while (current != null && depth < 20)
                            {
                                // 检查当前元素是否在排除元素列表中
                                for (Element excludeElement : excludeElements)
                                {
                                    if (excludeElement.equals(current))
                                    {
                                        shouldExclude = true;
                                        log.info("图片 {} 位于排除选择器匹配的元素内（深度: {}，元素: {}）", 
                                            imgSrc, depth, excludeElement.tagName() + (excludeElement.className().isEmpty() ? "" : "." + excludeElement.className()));
                                        break;
                                    }
                                }
                                
                                if (shouldExclude)
                                {
                                    break;
                                }
                                
                                // 也检查当前元素是否匹配排除选择器本身
                                for (String selector : excludeSelectors)
                                {
                                    selector = selector.trim();
                                    if (selector.isEmpty()) continue;
                                    
                                    try
                                    {
                                        if (current.is(selector))
                                        {
                                            shouldExclude = true;
                                            log.info("图片 {} 的祖先元素（深度: {}）匹配排除选择器: {}", imgSrc, depth, selector);
                                            break;
                                        }
                                    }
                                    catch (Exception e)
                                    {
                                        // 忽略选择器错误
                                    }
                                }
                                
                                if (shouldExclude)
                                {
                                    break;
                                }
                                
                                current = current.parent();
                                depth++;
                            }
                            
                            if (!shouldExclude)
                            {
                                filteredElements.add(img);
                            }
                            else
                            {
                                log.info("图片 {} 被排除选择器过滤", imgSrc);
                            }
                        }
                        
                        imgElements = filteredElements;
                        log.info("详情页 {} 使用用户配置的排除选择器: {}，过滤后剩余 {} 个图片元素（原始: {} 个，过滤: {} 个）", 
                            url, excludeSelector, imgElements.size(), allImgs.size(), allImgs.size() - imgElements.size());
                    }
                    else
                    {
                        // 只配置了正文容器，提取所有图片
                        imgElements = contentRoot.select("img");
                        log.info("详情页 {} 从用户配置的正文容器中提取所有图片，找到 {} 个图片元素", url, imgElements.size());
                    }
                }
                else
                {
                    // 关键改动：用户显式配置了正文容器选择器 => 不回退到默认/整页，避免误抓正文外图片
                    log.warn("详情页 {} 使用用户配置的正文容器选择器: {}，但未找到任何匹配容器，已跳过默认/整页回退（避免误抓正文外图片）", url, contentSelector);
                    imgElements = new Elements();
                }
            }
            else
            {
                // 用户未配置，使用默认逻辑
                // 尝试多种可能的正文容器选择器
                Element contentRoot = doc.selectFirst("#conttpc, .tpc_content, #content, .content, article, main, .post-content, .article-content");
                log.debug("详情页 {} 使用默认正文容器选择器，contentRoot: {}", url, contentRoot != null ? "找到" : "未找到");
                
                if (contentRoot != null)
                {
                    // 从正文容器中提取所有图片
                    imgElements = contentRoot.select("img");
                    log.info("详情页 {} 在默认正文容器中找到 {} 个图片元素", url, imgElements.size());
                }
                else
                {
                    // 如果找不到正文容器，从 body 提取所有图片
                    imgElements = doc.select("body img");
                    log.info("详情页 {} 在 body 中找到 {} 个图片元素", url, imgElements.size());
                }
            }
            int imageCount = 0;
            int filteredCount = 0;
            int emptyUrlCount = 0;
            Set<String> savedUrls = new HashSet<>();
            log.info("开始处理 {} 个图片元素", imgElements.size());
            for (Element img : imgElements)
            {
                // 在循环中检查停止标志（每处理一张图片前检查）
                if (!runningTasks.getOrDefault(task.getId(), false))
                {
                    log.info("任务 {} 已收到停止请求，停止提取图片", task.getId());
                    break;
                }
                
                // 检查元素是否是 img 元素
                if (!"img".equalsIgnoreCase(img.tagName()))
                {
                    log.warn("详情页 {} 选择器选择到的元素不是 img 元素，而是 {}，跳过", url, img.tagName());
                    emptyUrlCount++;
                    continue;
                }
                
                String imgSrc = extractImageUrlFromElement(img, url, doc.baseUri());
                
                if (imgSrc != null && !imgSrc.isEmpty())
                {
                    String normalized = normalizeUrl(imgSrc);
                    if (savedUrls.contains(normalized))
                    {
                        log.debug("图片 {} 已存在，跳过", imgSrc);
                        continue;
                    }

                    // 过滤掉小图标、logo等
                    // 如果用户配置了图片选择器，说明已经指定了正确的图片，应该完全信任用户的选择
                    // 如果只配置了正文容器选择器，说明用户指定了区域，过滤应该更宽松
                    boolean hasImageSelector = imageSelector != null && !imageSelector.trim().isEmpty();
                    boolean hasContentSelector = contentSelector != null && !contentSelector.trim().isEmpty();
                    boolean hasUserSelector = hasImageSelector || hasContentSelector 
                        || (excludeSelector != null && !excludeSelector.trim().isEmpty());
                    
                    log.debug("检查图片: {} - hasImageSelector: {}, hasContentSelector: {}", imgSrc, hasImageSelector, hasContentSelector);
                    boolean isValid = isValidImage(imgSrc, img, hasImageSelector, hasContentSelector);
                    if (isValid)
                    {
                        log.info("找到有效图片: {} (articleId: {})", imgSrc, articleId);
                        CrawlerImageEntity image = new CrawlerImageEntity();
                        image.setTaskId(task.getId());
                        image.setArticleId(articleId); // 关联到文章
                        image.setUrl(imgSrc);
                        image.setUrlHash(DigestUtil.md5Hex(imgSrc));
                        image.setDownloadStatus("PENDING");
                        image.setFormat(extractFileExtension(imgSrc));
                        image.setCreateTime(new Date());
                        imageService.save(image);

                        // 如果任务开启了"下载图片"，则尝试立即下载到本地
                        if (task.getDownloadImages() != null && task.getDownloadImages() == 1)
                        {
                            downloadImageToLocal(task, articleId, image, imgSrc, url);
                        }

                        imageCount++;
                        // 注意：successCount 用于统计URL爬取成功数，不是图片数量
                        savedUrls.add(normalized);
                    }
                    else
                    {
                        filteredCount++;
                        log.info("图片 {} 被过滤（不符合有效图片条件）", imgSrc);
                    }
                }
                else
                {
                    emptyUrlCount++;
                    log.debug("图片元素提取URL为空或null");
                }
            }
            
            if (imageCount > 0)
            {
                log.info("从详情页 {} 提取到 {} 张图片（articleId: {}）", url, imageCount, articleId);
            }
            else
            {
                log.warn("从详情页 {} 未提取到任何图片（articleId: {}）。统计：找到 {} 个图片元素，{} 个URL为空，{} 个被过滤", 
                    url, articleId, imgElements.size(), emptyUrlCount, filteredCount);
            }
        }
        catch (Exception e)
        {
            log.error("从详情页提取图片失败: {}", url, e);
        }
    }

    /**
     * 从多个匹配的正文容器中选择“最像正文”的那个（避免 selectFirst 选到 header/sidebar 等）
     * 规则：选择在容器内匹配 imageSelector 数量最多的；若并列则选文本更长的。
     */
    private Element selectBestContentRoot(Document doc, String contentSelector, String imageSelector)
    {
        try
        {
            Elements roots = doc.select(contentSelector);
            if (roots == null || roots.isEmpty())
            {
                return null;
            }
            Element best = null;
            int bestImgCount = -1;
            int bestTextLen = -1;
            String imgSel = (imageSelector == null || imageSelector.isBlank()) ? "img" : imageSelector;

            for (Element root : roots)
            {
                int imgCount;
                try
                {
                    imgCount = root.select(imgSel).size();
                }
                catch (Exception e)
                {
                    // imageSelector 可能不是纯 img 选择器（例如用户误配），兜底用 img
                    imgCount = root.select("img").size();
                }
                int textLen = root.text() != null ? root.text().length() : 0;

                if (imgCount > bestImgCount || (imgCount == bestImgCount && textLen > bestTextLen))
                {
                    best = root;
                    bestImgCount = imgCount;
                    bestTextLen = textLen;
                }
            }
            return best;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String extractImageUrlFromElement(Element img, String pageUrl, String baseUri)
    {
        // 常规 src
        String imgSrc = img.attr("abs:src");

        // 懒加载/常见替代字段
        if (imgSrc == null || imgSrc.isEmpty())
        {
            imgSrc = img.attr("abs:data-src");
        }
        if (imgSrc == null || imgSrc.isEmpty())
        {
            imgSrc = img.attr("abs:data-original");
        }

        // 特定站点：真实图片URL可能在 ess-data / data-link 中
        if (imgSrc == null || imgSrc.isEmpty())
        {
            imgSrc = resolveMaybeRelativeUrl(img.attr("ess-data"), pageUrl, baseUri);
        }
        if (imgSrc == null || imgSrc.isEmpty())
        {
            imgSrc = resolveMaybeRelativeUrl(img.attr("data-link"), pageUrl, baseUri);
        }

        // 兜底：避免抓到广告拦截占位图
        if (imgSrc != null && imgSrc.contains("adblo_ck"))
        {
            return "";
        }

        return imgSrc;
    }

    private String resolveMaybeRelativeUrl(String rawUrl, String pageUrl, String baseUri)
    {
        if (rawUrl == null)
        {
            return "";
        }
        String trimmed = rawUrl.trim();
        if (trimmed.isEmpty())
        {
            return "";
        }
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://"))
        {
            return trimmed;
        }
        if (trimmed.startsWith("//"))
        {
            String scheme = pageUrl != null && pageUrl.startsWith("https://") ? "https:" : "http:";
            return scheme + trimmed;
        }
        try
        {
            String base = (baseUri != null && !baseUri.isEmpty()) ? baseUri : pageUrl;
            if (base == null || base.isEmpty())
            {
                return trimmed;
            }
            return new URI(base).resolve(trimmed).toString();
        }
        catch (Exception e)
        {
            return trimmed;
        }
    }

    /**
     * 将图片下载到本地，并更新 crawler_image 记录。
     * 当 articleId 不为空时，每篇文章使用「文章ID_时间戳」单独目录。
     */
    private void downloadImageToLocal(CrawlerTaskEntity task,
                                      Long articleId,
                                      CrawlerImageEntity image,
                                      String imgSrc,
                                      String pageUrl)
    {
        // 最多重试3次
        int maxRetries = task.getMaxRetries() != null ? task.getMaxRetries() : 3;
        Exception lastException = null;
        
        for (int attempt = 0; attempt <= maxRetries; attempt++)
        {
            try
            {
                if (attempt > 0)
                {
                    // 重试前等待，使用指数退避策略
                    long waitTime = (long) Math.pow(2, attempt) * 1000; // 2秒, 4秒, 8秒
                    Thread.sleep(waitTime);
                    log.info("图片下载第 {} 次重试: {}", attempt, imgSrc);
                }
                
                if (imageBasePath == null || imageBasePath.isEmpty())
                {
                    imageBasePath = "./data/crawler-images";
                }

                Path baseDir = Paths.get(imageBasePath).toAbsolutePath().normalize();
                Files.createDirectories(baseDir);

                Path articleDir;
                if (articleId != null)
                {
                    articleDir = resolveArticleImageDir(articleId);
                }
                else
                {
                    // 纯图片任务：放在任务级目录下
                    String folderName = "task_" + task.getId();
                    articleDir = baseDir.resolve(folderName);
                }
                Files.createDirectories(articleDir);

                // 清理URL用于提取扩展名（移除查询参数）
                String cleanImgSrc = cleanUrlForFilename(imgSrc);
                String extension = extractFileExtension(cleanImgSrc);
                String fileName;
                if (image.getId() != null)
                {
                    fileName = image.getId() + (extension != null && !"unknown".equals(extension) ? "." + extension : "");
                }
                else
                {
                    fileName = DigestUtil.md5Hex(cleanImgSrc + System.currentTimeMillis())
                            + (extension != null && !"unknown".equals(extension) ? "." + extension : "");
                }
                
                // 清理文件名，移除所有非法字符（Windows/Linux文件系统不允许的字符）
                fileName = sanitizeFileName(fileName);

                Path targetFile = articleDir.resolve(fileName);

                // 执行图片下载请求前检查停止标志
                if (!runningTasks.getOrDefault(task.getId(), false))
                {
                    log.info("任务 {} 已收到停止请求，取消图片下载: {}", task.getId(), imgSrc);
                    image.setDownloadStatus("FAILED");
                    image.setErrorMsg("任务已停止");
                    imageService.updateById(image);
                    return;
                }

                // 增加超时时间，图片下载通常比页面下载慢
                int timeout = task.getRequestTimeout() != null ? task.getRequestTimeout() * 2 : 60000; // 默认60秒
                
                // 配置SSL（对于23img.com等可能有证书问题的站点）
                configureSslForImageDownload(imgSrc);
                
                // 解析代理列表（任务优先，其次全局）
                EffectiveProxyList effectiveProxyList = parseProxyList(task);
                
                // SOCKS 代理下部分站点会出现 TLS 握手被终止（SSLHandshakeException）。
                // Clash mixed-port 同时支持 SOCKS/HTTP，遇到该问题时自动降级为 HTTP CONNECT 代理（同 host:port）。
                boolean forceHttpProxyForTls = false;
                
                // 创建更真实的下载请求
                org.jsoup.Connection connection = Jsoup.connect(imgSrc)
                        .userAgent(getUserAgent(task, attempt))
                        .timeout(timeout)
                        .ignoreContentType(true)
                        .maxBodySize(50 * 1024 * 1024) // 增加到50MB
                        .followRedirects(true);

                // 设置代理（如果启用）
                ProxyInfo requestProxyInfo = null;
                String requestProxyTypeForThisAttempt = null;
                if (task.getUseProxy() != null && task.getUseProxy() == 1)
                {
                    if (effectiveProxyList.proxies.isEmpty())
                    {
                        // 任务启用代理但未配置/无启用全局代理：明确提示
                        if (attempt == 0)
                        {
                            log.warn("任务 {} 图片下载已启用代理(useProxy=1)，但未找到可用代理（source={}）。本次请求将直连，可能导致防爬虫拦截。", task.getId(), effectiveProxyList.source);
                        }
                    }
                    else
                    {
                        ProxyInfo proxyInfo = effectiveProxyList.proxies.get(ThreadLocalRandom.current().nextInt(effectiveProxyList.proxies.size()));
                        requestProxyInfo = proxyInfo;
                        requestProxyTypeForThisAttempt = forceHttpProxyForTls ? "HTTP" : proxyInfo.type;
                        Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(requestProxyTypeForThisAttempt) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                        Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyInfo.host, proxyInfo.port));
                        connection.proxy(proxy);

                        if (attempt == 0)
                        {
                            log.info("任务 {} 图片下载使用{}代理({}) {}:{}",
                                    task.getId(),
                                    effectiveProxyList.source,
                                    requestProxyTypeForThisAttempt,
                                    proxyInfo.host,
                                    proxyInfo.port);
                        }

                        if (proxyInfo.username != null && !proxyInfo.username.isEmpty())
                        {
                            // 代理认证：通过全局 Authenticator 处理（对 HttpURLConnection/Jsoup 生效）
                            String password = proxyInfo.password != null ? proxyInfo.password : "";
                            Authenticator.setDefault(new Authenticator()
                            {
                                @Override
                                protected PasswordAuthentication getPasswordAuthentication()
                                {
                                    return new PasswordAuthentication(proxyInfo.username, password.toCharArray());
                                }
                            });
                        }
                    }
                }

                // 对于23img.com，使用更简单的请求头（避免触发防爬虫）
                if (imgSrc.contains("23img.com"))
                {
                    // 23img.com可能需要从自己的域名访问，设置Referer为23img.com
                    connection.header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                    connection.header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                    // 不设置Sec-Fetch-*头，可能触发防爬虫
                    // 设置Referer为23img.com的首页
                    try
                    {
                        URI uri = new URI(imgSrc);
                        String baseUrl = uri.getScheme() + "://" + uri.getHost();
                        connection.referrer(baseUrl);
                    }
                    catch (Exception e)
                    {
                        // ignore
                    }
                }
                else
                {
                    // 其他图床使用完整的浏览器请求头
                    connection.header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                    connection.header("Accept-Encoding", "gzip, deflate, br");
                    connection.header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                    connection.header("Cache-Control", "no-cache");
                    connection.header("Pragma", "no-cache");
                    connection.header("Sec-Fetch-Dest", "image");
                    connection.header("Sec-Fetch-Mode", "no-cors");
                    connection.header("Sec-Fetch-Site", "cross-site");
                    connection.header("sec-ch-ua", "\"Google Chrome\";v=\"120\", \"Chromium\";v=\"120\", \"Not:A-Brand\";v=\"99\"");
                    connection.header("sec-ch-ua-mobile", "?0");
                    connection.header("sec-ch-ua-platform", "\"Windows\"");
                }

                // 设置 Referer（在23img.com的特殊处理之后，允许覆盖）
                if (task.getReferer() != null && !task.getReferer().isEmpty())
                {
                    connection.referrer(task.getReferer());
                }
                else if (pageUrl != null && !pageUrl.isEmpty() && !imgSrc.contains("23img.com"))
                {
                    // 对于非23img.com，使用页面URL作为Referer
                    connection.referrer(pageUrl);
                }

                addCustomHeaders(connection, task);

                // 执行下载
                log.debug("开始下载图片: {} (尝试 {}/{})", imgSrc, attempt + 1, maxRetries + 1);
                org.jsoup.Connection.Response resp;
                try
                {
                    resp = connection.execute();
                    // 成功后清理 TLS 降级标志
                    forceHttpProxyForTls = false;
                }
                catch (java.net.SocketTimeoutException | java.net.ConnectException | 
                       java.net.UnknownHostException | SSLException e)
                {
                    // 若使用 SOCKS 代理发生 TLS 握手错误，尝试切换为 HTTP 代理重试
                    if (!forceHttpProxyForTls
                            && requestProxyInfo != null
                            && "SOCKS".equalsIgnoreCase(requestProxyTypeForThisAttempt)
                            && (e instanceof SSLHandshakeException
                            || (e.getMessage() != null && e.getMessage().toLowerCase().contains("handshake"))))
                    {
                        forceHttpProxyForTls = true;
                        log.warn("任务 {} 图片下载走 SOCKS 代理发生 TLS 握手失败，下一次尝试将降级为 HTTP 代理: {}:{}（错误: {}）",
                                task.getId(),
                                requestProxyInfo.host,
                                requestProxyInfo.port,
                                e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName());
                        // 继续重试
                        if (attempt < maxRetries)
                        {
                            continue;
                        }
                        else
                        {
                            throw e;
                        }
                    }
                    else
                    {
                        throw e;
                    }
                }

                int statusCode = resp.statusCode();
                String contentType = resp.contentType();

                if (statusCode != 200)
                {
                    throw new IOException("HTTP状态码: " + statusCode);
                }

                // 放宽 Content-Type 检查，有些图床返回的 Content-Type 不标准
                if (contentType != null && contentType.toLowerCase().contains("text/html"))
                {
                    // 如果返回 HTML，可能是防爬虫页面或重定向页面
                    // 特殊处理：尝试从页面中提取真实图片URL
                    String htmlContent = resp.body();
                    String responseUrl = resp.url().toString();
                    
                    // 记录HTML内容用于调试（完整内容，便于分析）
                    if (htmlContent != null && !htmlContent.isEmpty())
                    {
                        // 输出完整HTML内容到日志（限制长度避免日志过大）
                        int maxLogLength = 5000;
                        String htmlForLog = htmlContent.length() > maxLogLength ? 
                            htmlContent.substring(0, maxLogLength) + "...[truncated]" : htmlContent;

                        // 同时输出关键信息：查找所有可能的图片URL
                        log.info("开始分析HTML，查找图片URL...");
                    }
                    
                    String realImageUrl = extractRealImageUrlFromHtml(imgSrc, htmlContent, responseUrl);
                    
                    if (realImageUrl != null && !realImageUrl.equals(imgSrc))
                    {
                        log.info("从HTML页面提取到真实图片URL: {} -> {}", imgSrc, realImageUrl);
                        // 使用提取到的真实URL重新构建连接并下载
                        imgSrc = realImageUrl;
                        
                        // 重新构建连接（使用相同的代理配置）
                        connection = Jsoup.connect(imgSrc)
                                .userAgent(getUserAgent(task, attempt))
                                .timeout(timeout)
                                .ignoreContentType(true)
                                .maxBodySize(50 * 1024 * 1024)
                                .followRedirects(true);
                        
                        // 重新设置代理（如果启用）
                        if (task.getUseProxy() != null && task.getUseProxy() == 1 && 
                            effectiveProxyList.proxies != null && !effectiveProxyList.proxies.isEmpty())
                        {
                            ProxyInfo proxyInfo = effectiveProxyList.proxies.get(
                                    ThreadLocalRandom.current().nextInt(effectiveProxyList.proxies.size()));
                            Proxy.Type proxyType = "SOCKS".equalsIgnoreCase(proxyInfo.type) ? Proxy.Type.SOCKS : Proxy.Type.HTTP;
                            Proxy proxy = new Proxy(proxyType, new InetSocketAddress(proxyInfo.host, proxyInfo.port));
                            connection.proxy(proxy);
                            
                            if (proxyInfo.username != null && !proxyInfo.username.isEmpty())
                            {
                                String password = proxyInfo.password != null ? proxyInfo.password : "";
                                Authenticator.setDefault(new Authenticator()
                                {
                                    @Override
                                    protected PasswordAuthentication getPasswordAuthentication()
                                    {
                                        return new PasswordAuthentication(proxyInfo.username, password.toCharArray());
                                    }
                                });
                            }
                        }
                        
                        // 重新设置请求头和Referer
                        connection.header("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
                        connection.header("Accept-Encoding", "gzip, deflate, br");
                        connection.header("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
                        connection.header("Cache-Control", "no-cache");
                        connection.header("Pragma", "no-cache");
                        connection.header("Sec-Fetch-Dest", "image");
                        connection.header("Sec-Fetch-Mode", "no-cors");
                        connection.header("Sec-Fetch-Site", "cross-site");
                        
                        if (pageUrl != null && !pageUrl.isEmpty())
                        {
                            connection.referrer(pageUrl);
                        }
                        else
                        {
                            try
                            {
                                URI uri = new URI(imgSrc);
                                String baseUrl = uri.getScheme() + "://" + uri.getHost();
                                connection.referrer(baseUrl);
                            }
                            catch (Exception e)
                            {
                                // ignore
                            }
                        }
                        
                        addCustomHeaders(connection, task);
                        
                        // 使用新的URL重新执行请求
                        try
                        {
                            resp = connection.execute();
                            forceHttpProxyForTls = false;
                            // 重新检查Content-Type
                            contentType = resp.contentType();
                            if (contentType == null || !contentType.toLowerCase().contains("text/html"))
                            {
                                // 成功获取到图片，继续处理
                            }
                            else
                            {
                                // 仍然返回HTML，可能无法绕过
                                throw new IOException("真实图片URL仍然返回HTML页面，Content-Type: " + contentType);
                            }
                        }
                        catch (java.net.SocketTimeoutException | java.net.ConnectException | 
                               java.net.UnknownHostException | SSLException e)
                        {
                            // 处理网络错误（与之前的逻辑相同）
                            if (!forceHttpProxyForTls
                                    && requestProxyInfo != null
                                    && "SOCKS".equalsIgnoreCase(requestProxyTypeForThisAttempt)
                                    && (e instanceof SSLHandshakeException
                                    || (e.getMessage() != null && e.getMessage().toLowerCase().contains("handshake"))))
                            {
                                forceHttpProxyForTls = true;
                                if (attempt < maxRetries)
                                {
                                    continue;
                                }
                            }
                            throw e;
                        }
                    }
                    else
                    {
                        // 无法提取真实URL
                        if (imgSrc.contains("23img.com") || imgSrc.contains("imgbb.com"))
                        {
                            // 对于23img.com，尝试多种URL变体
                            if (imgSrc.contains("23img.com") && imgSrc.contains("/i/"))
                            {
                                // 移除查询参数，尝试直接访问
                                String cleanUrl = cleanUrlForFilename(imgSrc);
                                if (!cleanUrl.equals(imgSrc) && attempt < maxRetries)
                                {
                                    log.info("尝试使用清理后的URL重新下载: {} -> {}", imgSrc, cleanUrl);
                                    imgSrc = cleanUrl;
                                    continue;
                                }
                                
                                // 如果清理后的URL还是失败，尝试添加不同的参数
                                String[] paramVariants = {"?v=1", "?t=" + System.currentTimeMillis(), "?nocache=1", "?s=1"};
                                for (String param : paramVariants)
                                {
                                    String variantUrl = cleanUrl + param;
                                    if (!variantUrl.equals(imgSrc) && attempt < maxRetries)
                                    {
                                        log.info("尝试URL变体: {}", variantUrl);
                                        imgSrc = variantUrl;
                                        continue;
                                    }
                                }
                            }
                            
                            log.warn("图床 {} 返回HTML页面，无法提取真实图片URL，疑似防爬虫或需要Cookie/JavaScript验证", imgSrc);
                            throw new IOException("图床防爬虫：返回HTML页面，无法提取真实图片URL，建议启用代理。Content-Type: " + contentType);
                        }
                        else
                        {
                            throw new IOException("返回HTML内容而非图片，Content-Type: " + contentType);
                        }
                    }
                }

                byte[] bytes = resp.bodyAsBytes();
                if (bytes == null || bytes.length == 0)
                {
                    throw new IOException("图片内容为空");
                }

                // 检查文件大小（过滤掉异常小的"图片"，可能是防爬虫返回的占位图）
                if (bytes.length < 1024)
                {
                    log.warn("图片文件过小 ({} bytes)，可能不是真实图片: {}", bytes.length, imgSrc);
                }

                Files.write(targetFile, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

                image.setFileName(fileName);
                image.setFilePath(articleDir.toString());
                image.setFileSize((long) bytes.length);
                image.setMd5(DigestUtil.md5Hex(bytes));
                image.setDownloadStatus("SUCCESS");
                imageService.updateById(image);
                
                log.debug("图片下载成功: {} -> {} ({} bytes)", imgSrc, targetFile, bytes.length);
                return; // 下载成功，退出
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                log.warn("图片下载被中断: {}", imgSrc);
                break;
            }
            catch (Exception e)
            {
                lastException = e;
                log.warn("图片下载失败 (尝试 {}/{}): {} -> {}", 
                        attempt + 1, maxRetries + 1, imgSrc, e.getMessage());
                
                // 如果不是最后一次重试，继续
                if (attempt < maxRetries)
                {
                    continue;
                }
            }
        }
        
        // 所有重试都失败
        try
        {
            image.setDownloadStatus("FAILED");
            image.setErrorMsg(lastException != null ? lastException.getMessage() : "下载失败");
            imageService.updateById(image);
            log.error("图片下载最终失败 (已重试 {} 次): {} -> {}", 
                    maxRetries, imgSrc, lastException != null ? lastException.getMessage() : "未知错误");
        }
        catch (Exception ignored)
        {
            // ignore
        }
    }

    /**
     * 根據文章ID獲取（或創建）圖片存儲目錄：文章ID_時間戳
     */
    private Path resolveArticleImageDir(Long articleId) throws IOException
    {
        String folder = articleImageFolderCache.computeIfAbsent(articleId, id -> {
            try
            {
                CrawlerArticleEntity article = articleService.getById(id);
                long ts = System.currentTimeMillis();
                if (article != null && article.getCreateTime() != null)
                {
                    ts = article.getCreateTime().getTime();
                }
                String formatted = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date(ts));
                return id + "_" + formatted;
            }
            catch (Exception e)
            {
                String formatted = new java.text.SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
                return id + "_" + formatted;
            }
        });

        Path baseDir = Paths.get(imageBasePath).toAbsolutePath().normalize();
        return baseDir.resolve(folder);
    }
    /**
     * 判断是否为有效图片（过滤明显的小图标、logo、广告横幅等）
     * @param imgSrc 图片URL
     * @param img 图片元素
     * @param hasImageSelector 用户是否配置了图片选择器（如果配置了，完全信任用户的选择）
     * @param hasContentSelector 用户是否配置了正文容器选择器（如果配置了，过滤应该更宽松）
     */
    private boolean isValidImage(String imgSrc, Element img, boolean hasImageSelector, boolean hasContentSelector)
    {
        String lowerSrc = imgSrc.toLowerCase();
        
        // 0. 全局规则：直接过滤所有 GIF 图片（详情页中间穿插的广告多为 gif）
        if (lowerSrc.endsWith(".gif")
                || lowerSrc.contains(".gif?")
                || lowerSrc.contains(".gif#")
                || lowerSrc.contains(".gif&"))
        {
            log.debug("图片 {} 被过滤（全局规则：过滤所有 GIF 图片）", imgSrc);
            return false;
        }
        
        // 1. URL 關鍵字：只過濾非常明確的縮略圖關鍵字，避免誤殺
        String[] strictThumbKeywords = {
            "/thumb/", "/thumbnail/", "/preview/", "/mini/", "/small/",
            "_thumb", "_thumbnail", "_preview", "_mini", "_small",
            "-thumb", "-thumbnail", "-preview", "-mini", "-small"
        };
        
        for (String keyword : strictThumbKeywords)
        {
            if (lowerSrc.contains(keyword))
            {
                log.debug("图片 {} 被过滤（URL包含明確的縮略圖關鍵字: {}）", imgSrc, keyword);
                return false;
            }
        }
        
        // 如果用户配置了图片选择器，说明已经指定了正确的图片，完全信任用户的选择
        // 只过滤 URL 中的明确缩略图关键字，不做其他任何过滤
        if (hasImageSelector)
        {
            log.info("图片 {} 通过验证（用户配置了图片选择器，完全信任用户选择）", imgSrc);
            return true;
        }
        
        // 如果用户只配置了正文容器选择器，说明用户指定了区域，过滤应该更宽松
        if (hasContentSelector)
        {
            // 只过滤明确的广告容器和侧边栏
            Element parent = img.parent();
            int depth = 0;
            while (parent != null && depth < 5)
            {
                String tagName = parent.tagName().toLowerCase();
                String className = parent.className().toLowerCase();
                String id = parent.id().toLowerCase();
                
                // 排除明確的側邊欄、廣告容器
                if ("aside".equals(tagName) || "nav".equals(tagName))
                {
                    log.debug("图片 {} 被过滤（位于 {} 標籤中）", imgSrc, tagName);
                    return false;
                }
                
                String[] strictExcludeContainers = {
                    "sidebar", "advertisement", "banner", "sponsor", "sponsored", "widget"
                };
                
                for (String container : strictExcludeContainers)
                {
                    if (className.equals(container) || id.equals(container)
                        || className.startsWith(container + "-") || className.startsWith(container + "_")
                        || id.startsWith(container + "-") || id.startsWith(container + "_"))
                    {
                        log.debug("图片 {} 被过滤（位于 {} 容器中）", imgSrc, container);
                        return false;
                    }
                }
                
                parent = parent.parent();
                depth++;
            }
            // 用户配置了正文容器选择器，信任用户的选择，不做其他过滤
            log.info("图片 {} 通过验证（用户配置了正文容器选择器）", imgSrc);
            return true;
        }
        
        // 用户未配置选择器，使用默认的过滤逻辑
        // 2. 检查是否在推荐/相关区域（通过文本内容和class/id）
        Element parent = img.parent();
        int depth = 0;
        boolean isInRecommendArea = false;
        String recommendAreaReason = "";
        
        while (parent != null && depth < 8)
        {
            String tagName = parent.tagName().toLowerCase();
            String className = parent.className().toLowerCase();
            String id = parent.id().toLowerCase();
            String parentText = parent.text().toLowerCase();
            String parentClassId = className + " " + id;
            
            // 检查文本内容是否包含推荐关键词
            boolean hasRecommendText = parentText.contains("相關美圖") || parentText.contains("相关美图")
                || parentText.contains("相關文章") || parentText.contains("相关文章")
                || parentText.contains("推薦作品") || parentText.contains("推荐作品")
                || parentText.contains("相關推薦") || parentText.contains("相关推荐")
                || parentText.contains("你可能也喜歡") || parentText.contains("你可能也喜欢")
                || parentText.contains("更多推薦") || parentText.contains("更多推荐");
            
            // 检查class/id是否包含推荐相关关键词（只检查明确的容器）
            boolean hasRecommendClass = parentClassId.contains("related-posts") || parentClassId.contains("related-articles")
                || parentClassId.contains("post-list") || parentClassId.contains("article-list")
                || parentClassId.contains("item-list") || parentClassId.contains("card-list")
                || parentClassId.contains("gallery-list") || parentClassId.contains("work-list");
            
            // 如果同时满足文本和class/id条件，或者文本非常明确（如"相關美圖"），则认为是推荐区域
            if (hasRecommendText && (hasRecommendClass || parentText.contains("相關美圖") || parentText.contains("相关美图")))
            {
                isInRecommendArea = true;
                recommendAreaReason = "文本包含推荐关键词且class/id匹配";
                break;
            }
            
            // 检查明确的推荐容器
            String[] strictRecommendContainers = {
                "related-posts", "related-articles", "related-images", "related-gallery"
            };
            
            for (String container : strictRecommendContainers)
            {
                if (className.equals(container) || id.equals(container)
                    || className.startsWith(container + "-") || className.startsWith(container + "_")
                    || id.startsWith(container + "-") || id.startsWith(container + "_"))
                {
                    isInRecommendArea = true;
                    recommendAreaReason = "位于明确的推荐容器: " + container;
                    break;
                }
            }
            
            if (isInRecommendArea)
            {
                break;
            }
            
            // 排除明確的側邊欄、廣告容器
            if ("aside".equals(tagName) || "nav".equals(tagName))
            {
                log.debug("图片 {} 被过滤（位于 {} 標籤中）", imgSrc, tagName);
                return false;
            }
            
            String[] strictExcludeContainers = {
                "sidebar", "advertisement", "banner", "sponsor", "sponsored", "widget"
            };
            
            for (String container : strictExcludeContainers)
            {
                if (className.equals(container) || id.equals(container)
                    || className.startsWith(container + "-") || className.startsWith(container + "_")
                    || id.startsWith(container + "-") || id.startsWith(container + "_"))
                {
                    log.debug("图片 {} 被过滤（位于 {} 容器中）", imgSrc, container);
                    return false;
                }
            }
            
            parent = parent.parent();
            depth++;
        }
        
        // 如果在推荐/相关区域，直接过滤
        if (isInRecommendArea)
        {
            log.debug("图片 {} 被过滤（位于推荐/相关区域: {}）", imgSrc, recommendAreaReason);
            return false;
        }
        
        // 3. 圖片自身 class/id：排除縮略圖、廣告、圖示等
        String imgClass = img.className().toLowerCase();
        String imgId = img.id().toLowerCase();
        String[] excludeImgAttributes = {
            "ad", "ads", "advertisement", "banner",
            "sponsor", "sponsored", "avatar", "icon",
            // 增強：排除縮略圖相關的 class/id
            "thumb", "thumbnail", "preview", "mini", "small",
            "thumb-img", "thumbnail-img", "preview-img"
        };
        
        for (String attr : excludeImgAttributes)
        {
            if (imgClass.contains(attr) || imgId.contains(attr))
            {
                // 如果是縮略圖相關的 class/id，直接過濾（不依賴尺寸）
                if (attr.contains("thumb") || attr.contains("preview") || 
                    attr.contains("mini") || attr.contains("small"))
                {
                    log.debug("图片 {} 被过滤（class/id包含縮略圖關鍵字: {}）", imgSrc, attr);
                    return false;
                }
                
                String widthAttr = img.attr("width");
                String heightAttr = img.attr("height");
                if (widthAttr != null && !widthAttr.isEmpty() && heightAttr != null && !heightAttr.isEmpty())
                {
                    try
                    {
                        int w = Integer.parseInt(widthAttr.replaceAll("[^0-9]", ""));
                        int h = Integer.parseInt(heightAttr.replaceAll("[^0-9]", ""));
                        if (w < 100 || h < 100)
                        {
                            return false;
                        }
                    }
                    catch (NumberFormatException e)
                    {
                        // 無法解析尺寸時，不因 class/id 關鍵字直接過濾
                    }
                }
            }
        }
        
        // 4. 通用尺寸規則：排除極小圖片/極端橫幅/縮略圖
        String widthAttr = img.attr("width");
        String heightAttr = img.attr("height");
        if (widthAttr != null && !widthAttr.isEmpty() && heightAttr != null && !heightAttr.isEmpty())
        {
            try
            {
                int w = Integer.parseInt(widthAttr.replaceAll("[^0-9]", ""));
                int h = Integer.parseInt(heightAttr.replaceAll("[^0-9]", ""));
                if (w > 0 && h > 0)
                {
                    // 避免抓到 1x1、很小的 icon / badge
                    if (w < 60 || h < 60)
                    {
                        log.debug("图片 {} 被过滤（尺寸過小: {}x{}）", imgSrc, w, h);
                        return false;
                    }
                    
                    // 增強：排除縮略圖尺寸（只過濾非常小的圖片，避免誤殺）
                    // 如果圖片很小（小於 150x150），且包含縮略圖關鍵字，則過濾
                    if (w < 150 && h < 150)
                    {
                        String imgClassId = imgClass + " " + imgId;
                        boolean hasThumbKeyword = lowerSrc.contains("thumb") || lowerSrc.contains("preview") 
                            || lowerSrc.contains("mini") || lowerSrc.contains("small")
                            || imgClassId.contains("thumb") || imgClassId.contains("preview")
                            || imgClassId.contains("mini") || imgClassId.contains("small");
                        
                        if (hasThumbKeyword)
                        {
                            log.debug("图片 {} 被过滤（小尺寸且包含縮略圖關鍵字: {}x{}）", imgSrc, w, h);
                            return false;
                        }
                    }
                    
                    // 非常長條的橫幅（例如 1000x100）
                    double aspectRatio = (double) Math.max(w, h) / Math.min(w, h);
                    if (aspectRatio > 6.0)
                    {
                        log.debug("图片 {} 被过滤（長寬比過大: {}）", imgSrc, aspectRatio);
                        return false;
                    }
                }
            }
            catch (NumberFormatException e)
            {
                // 忽略，交由其他規則
            }
        }
        
        return true;
    }
    
    /**
     * 页面类型枚举
     */
    private enum PageType
    {
        LIST,   // 列表页
        DETAIL, // 详情页
        MIXED   // 混合页（既有列表又有详情）
    }
    
    /**
     * 配置SSL，忽略证书验证（仅用于特定站点如23img.com）
     * 注意：这会降低安全性，仅用于处理证书问题的站点
     */
    private void configureSslForImageDownload(String imageUrl)
    {
        // 仅对特定站点忽略SSL验证
        if (imageUrl != null && (imageUrl.contains("23img.com") || imageUrl.contains("imgbb.com")))
        {
            try
            {
                // 创建信任所有证书的TrustManager
                TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                        public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                    }
                };
                
                // 创建SSLContext并初始化
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, trustAllCerts, new java.security.SecureRandom());
                
                // 设置默认的SSLSocketFactory和HostnameVerifier
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
                HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
            }
            catch (Exception e)
            {
                log.debug("配置SSL失败: {}", e.getMessage());
            }
        }
    }
    
    /**
     * 从HTML页面或URL参数中提取真实图片URL
     * 用于处理23img.com等图床的重定向机制
     */
    private String extractRealImageUrlFromHtml(String originalUrl, String htmlContent, String responseUrl)
    {
        try
        {
            // 策略1：从URL参数中提取（23img.com使用 /l/?i= 格式）
            if (originalUrl.contains("23img.com") && originalUrl.contains("/l/?i="))
            {
                // 从URL参数中提取 i 参数的值
                int iParamIndex = originalUrl.indexOf("?i=");
                if (iParamIndex > 0)
                {
                    String paramValue = originalUrl.substring(iParamIndex + 3);
                    // 移除可能的其他参数（如 &s=1）
                    int paramEnd = paramValue.indexOf("&");
                    if (paramEnd > 0)
                    {
                        paramValue = paramValue.substring(0, paramEnd);
                    }
                    // 移除URL编码（如果有）
                    paramValue = java.net.URLDecoder.decode(paramValue, "UTF-8");
                    
                    // 构建真实图片URL
                    if (paramValue.startsWith("/"))
                    {
                        // 相对路径，需要添加域名
                        String baseUrl = extractBaseUrl(originalUrl);
                        return baseUrl + paramValue;
                    }
                    else if (paramValue.startsWith("http://") || paramValue.startsWith("https://"))
                    {
                        // 绝对URL
                        return paramValue;
                    }
                    else
                    {
                        // 相对路径，需要添加域名和路径前缀
                        String baseUrl = extractBaseUrl(originalUrl);
                        return baseUrl + "/" + paramValue;
                    }
                }
            }
            
            // 策略2：从HTML中解析img标签或JavaScript变量
            if (htmlContent != null && !htmlContent.isEmpty())
            {
                try
                {
                    // 确保baseUri正确设置（使用原始URL的域名）
                    String baseUri = responseUrl;
                    if (baseUri == null || baseUri.isEmpty())
                    {
                        baseUri = originalUrl;
                    }
                    // 如果baseUri是重定向后的URL（如 /l/?i=），使用原始URL的域名
                    if (baseUri.contains("/l/"))
                    {
                        baseUri = extractBaseUrl(originalUrl);
                    }
                    
                    log.debug("解析HTML，baseUri: {}, responseUrl: {}", baseUri, responseUrl);
                    Document doc = Jsoup.parse(htmlContent, baseUri);
                    
                    // 优先查找最大的img标签（通常是主图）
                    Elements imgElements = doc.select("img[src], img[data-src], img[data-original]");
                    log.debug("找到 {} 个img标签", imgElements.size());
                    
                    // 输出所有img标签的src属性用于调试
                    for (int i = 0; i < imgElements.size() && i < 10; i++)
                    {
                        Element img = imgElements.get(i);
                        String srcAttr = img.attr("src");
                        String dataSrcAttr = img.attr("data-src");
                        String dataOriginalAttr = img.attr("data-original");
                        String absSrc = img.attr("abs:src");
                        log.debug("img[{}]: src='{}', data-src='{}', data-original='{}', abs:src='{}'", 
                                i, srcAttr, dataSrcAttr, dataOriginalAttr, absSrc);
                    }
                    
                    Element largestImg = null;
                    int maxSize = 0;
                    
                    for (Element img : imgElements)
                    {
                        // 尝试多个属性
                        String src = img.attr("abs:src");
                        if (src == null || src.isEmpty())
                        {
                            src = img.attr("abs:data-src");
                        }
                        if (src == null || src.isEmpty())
                        {
                            src = img.attr("abs:data-original");
                        }
                        
                        if (src != null && !src.isEmpty())
                        {
                            // 排除明显不是目标图片的元素
                            String lowerSrc = src.toLowerCase();
                            if (lowerSrc.contains("logo") || lowerSrc.contains("icon") || 
                                lowerSrc.contains("avatar") || lowerSrc.contains("banner") ||
                                lowerSrc.contains("ad") || lowerSrc.contains("sponsor"))
                            {
                                continue;
                            }
                            
                            // 计算图片尺寸（用于选择最大的图片）
                            String width = img.attr("width");
                            String height = img.attr("height");
                            int size = 0;
                            if (width != null && !width.isEmpty() && height != null && !height.isEmpty())
                            {
                                try
                                {
                                    int w = Integer.parseInt(width.replaceAll("[^0-9]", ""));
                                    int h = Integer.parseInt(height.replaceAll("[^0-9]", ""));
                                    size = w * h;
                                }
                                catch (NumberFormatException e)
                                {
                                    // 如果无法解析尺寸，使用默认值
                                    size = 1000;
                                }
                            }
                            else
                            {
                                // 如果没有尺寸信息，检查URL中是否包含图片扩展名
                                if (src.contains(".jpg") || src.contains(".jpeg") || 
                                    src.contains(".png") || src.contains(".gif") || 
                                    src.contains(".webp"))
                                {
                                    size = 1000; // 默认值
                                }
                            }
                            
                            if (size > maxSize)
                            {
                                maxSize = size;
                                largestImg = img;
                            }
                        }
                    }
                    
                    if (largestImg != null)
                    {
                        String src = largestImg.attr("abs:src");
                        if (src == null || src.isEmpty())
                        {
                            src = largestImg.attr("abs:data-src");
                        }
                        if (src == null || src.isEmpty())
                        {
                            src = largestImg.attr("abs:data-original");
                        }
                        if (src != null && !src.isEmpty())
                        {
                            log.info("从最大的img标签提取到URL: {}", src);
                            return src;
                        }
                    }
                    
                    // 如果没找到最大的，尝试查找所有包含图片扩展名的img标签
                    for (Element img : imgElements)
                    {
                        String src = img.attr("abs:src");
                        String relSrc = img.attr("src");
                        
                        log.debug("处理img标签: abs:src='{}', src='{}'", src, relSrc);
                        
                        // 如果abs:src为空，尝试手动处理相对路径
                        if (src == null || src.isEmpty())
                        {
                            if (relSrc != null && !relSrc.isEmpty())
                            {
                                // 手动转换为绝对路径
                                if (relSrc.startsWith("/"))
                                {
                                    // 绝对路径，直接拼接域名
                                    String domain = baseUri.replaceAll("https?://([^/]+).*", "$1");
                                    src = (baseUri.startsWith("https") ? "https://" : "http://") + domain + relSrc;
                                    log.debug("转换绝对路径: {} -> {}", relSrc, src);
                                }
                                else if (relSrc.startsWith("http://") || relSrc.startsWith("https://"))
                                {
                                    src = relSrc;
                                }
                                else
                                {
                                    // 相对路径
                                    try
                                    {
                                        URI baseUriObj = new URI(baseUri);
                                        src = baseUriObj.resolve(relSrc).toString();
                                        log.debug("转换相对路径: {} -> {}", relSrc, src);
                                    }
                                    catch (Exception e)
                                    {
                                        log.debug("解析相对路径失败: {}", e.getMessage());
                                    }
                                }
                            }
                            
                            // 如果还是空的，尝试data-src和data-original
                            if (src == null || src.isEmpty())
                            {
                                src = img.attr("abs:data-src");
                            }
                            if (src == null || src.isEmpty())
                            {
                                src = img.attr("abs:data-original");
                            }
                        }
                        
                        if (src != null && !src.isEmpty())
                        {
                            log.debug("提取到URL: {}", src);
                            
                            // 对于23img.com，优先选择包含 /i/ 路径的URL（这是23img.com图片的特征路径）
                            if (originalUrl.contains("23img.com"))
                            {
                                if (src.contains("/i/"))
                                {
                                    log.info("从img标签提取到23img.com图片URL（包含/i/路径）: {}", src);
                                    return src;
                                }
                                // 如果URL包含23img.com域名，也接受
                                if (src.contains("23img.com"))
                                {
                                    log.info("从img标签提取到23img.com图片URL: {}", src);
                                    return src;
                                }
                            }
                            
                            // 检查是否包含图片扩展名
                            if (src.contains(".jpg") || src.contains(".jpeg") || 
                                src.contains(".png") || src.contains(".gif") || 
                                src.contains(".webp"))
                            {
                                String lowerSrc = src.toLowerCase();
                                if (!lowerSrc.contains("logo") && !lowerSrc.contains("icon") && 
                                    !lowerSrc.contains("avatar") && !lowerSrc.contains("banner"))
                                {
                                    log.info("从img标签提取到图片URL: {}", src);
                                    return src;
                                }
                            }
                        }
                    }
                    
                    // 尝试从JavaScript中提取图片URL（扩展更多模式）
                    // 查找包含图片URL的JavaScript变量或函数调用
                    String[] jsPatterns = {
                        "src\\s*[:=]\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        "url\\s*[:=]\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        "image\\s*[:=]\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        "href\\s*[:=]\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        "loadImage\\s*\\(\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        "showImage\\s*\\(\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        "imgUrl\\s*[:=]\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        "pictureUrl\\s*[:=]\\s*['\"]([^'\"]+\\.(jpg|jpeg|png|gif|webp))['\"]",
                        // 查找所有包含23img.com的URL
                        "(https?://[^/]*23img\\.com[^'\"\\s<>)]+\\.(jpg|jpeg|png|gif|webp)[^'\"\\s<>)]*)"
                    };
                    
                    for (String pattern : jsPatterns)
                    {
                        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, 
                                java.util.regex.Pattern.CASE_INSENSITIVE);
                        java.util.regex.Matcher matcher = regex.matcher(htmlContent);
                        if (matcher.find())
                        {
                            String foundUrl = matcher.group(1);
                            if (foundUrl != null && !foundUrl.isEmpty())
                            {
                                // 如果是相对路径，转换为绝对路径
                                if (foundUrl.startsWith("/"))
                                {
                                    String baseUrl = extractBaseUrl(originalUrl);
                                    return baseUrl + foundUrl;
                                }
                                else if (foundUrl.startsWith("http://") || foundUrl.startsWith("https://"))
                                {
                                    return foundUrl;
                                }
                                else
                                {
                                    // 相对路径
                                    try
                                    {
                                        URI baseUriObj = new URI(responseUrl);
                                        return baseUriObj.resolve(foundUrl).toString();
                                    }
                                    catch (Exception e)
                                    {
                                        // ignore
                                    }
                                }
                            }
                        }
                    }
                }
                catch (Exception e)
                {
                    log.debug("解析HTML提取图片URL失败: {}", e.getMessage());
                }
            }
            
            // 策略3：对于23img.com，尝试从HTML中查找所有可能的图片URL模式
            if (originalUrl.contains("23img.com") && htmlContent != null && !htmlContent.isEmpty())
            {
                log.debug("策略3：在HTML中搜索23img.com的图片URL");
                
                // 查找所有包含23img.com的图片URL（更宽松的正则）
                java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(
                    "https?://[^/]*23img\\.com[^\\s\"'<>)]+\\.(jpg|jpeg|png|gif|webp)(?:\\?[^\\s\"'<>)]*)?",
                    java.util.regex.Pattern.CASE_INSENSITIVE);
                java.util.regex.Matcher urlMatcher = urlPattern.matcher(htmlContent);
                
                List<String> foundUrls = new ArrayList<>();
                while (urlMatcher.find())
                {
                    String foundUrl = urlMatcher.group();
                    log.debug("找到可能的图片URL: {}", foundUrl);
                    // 排除明显不是目标图片的URL
                    String lowerUrl = foundUrl.toLowerCase();
                    if (!lowerUrl.contains("logo") && !lowerUrl.contains("icon") && 
                        !lowerUrl.contains("avatar") && !lowerUrl.contains("banner") &&
                        !lowerUrl.contains("ad") && !lowerUrl.contains("sponsor"))
                    {
                        foundUrls.add(foundUrl);
                    }
                }
                
                // 如果正则没找到，尝试查找所有包含 /i/ 路径的URL（23img.com的图片路径特征）
                if (foundUrls.isEmpty())
                {
                    log.debug("正则未找到URL，尝试查找包含 /i/ 路径的URL");
                    java.util.regex.Pattern pathPattern = java.util.regex.Pattern.compile(
                        "(https?://[^/]*23img\\.com/i/[^\\s\"'<>)]+)",
                        java.util.regex.Pattern.CASE_INSENSITIVE);
                    java.util.regex.Matcher pathMatcher = pathPattern.matcher(htmlContent);
                    while (pathMatcher.find())
                    {
                        String foundPath = pathMatcher.group(1);
                        // 检查是否以图片扩展名结尾
                        if (foundPath.matches(".*\\.(jpg|jpeg|png|gif|webp)(?:\\?.*)?$"))
                        {
                            log.debug("找到包含 /i/ 路径的图片URL: {}", foundPath);
                            foundUrls.add(foundPath);
                        }
                    }
                }
                
                    if (!foundUrls.isEmpty())
                    {
                        log.info("从HTML中找到 {} 个可能的图片URL", foundUrls.size());
                        // 优先选择包含原始路径的URL
                        String originalPath = originalUrl.contains("/i/") ? 
                            originalUrl.substring(originalUrl.indexOf("/i/")) : null;
                        if (originalPath != null)
                        {
                            // 清理路径（移除查询参数和片段）
                            String cleanPath = originalPath.split("\\?")[0].split("#")[0];
                            for (String url : foundUrls)
                            {
                                if (url.contains(cleanPath))
                                {
                                    log.info("从HTML中找到匹配原始路径的图片URL: {}", url);
                                    return url;
                                }
                            }
                        }
                        
                        // 如果没有匹配的，返回第一个找到的URL
                        log.info("使用第一个找到的图片URL: {}", foundUrls.get(0));
                        return foundUrls.get(0);
                    
                    // 如果没有匹配的，返回第一个找到的URL
                    }
                
                // 如果URL是 /i/ 开头的路径，可能是直接图片URL
                if (originalUrl.contains("/i/") && !originalUrl.contains("/l/"))
                {
                    // 直接访问的图片URL，但返回了HTML
                    // 尝试从原始URL构建（移除查询参数后重新添加）
                    String baseUrl = extractBaseUrl(originalUrl);
                    String path = originalUrl.substring(originalUrl.indexOf("/i/"));
                    // 移除查询参数
                    int queryIndex = path.indexOf("?");
                    if (queryIndex > 0)
                    {
                        path = path.substring(0, queryIndex);
                    }
                    
                    // 尝试几种可能的真实图片URL格式
                    String[] possibleUrls = {
                        baseUrl + path,  // 原始路径（无参数）
                        baseUrl + path + "?s=1",  // 带s=1参数（常见）
                        baseUrl + path + "?v=1",  // 带版本参数
                        baseUrl + path + "?t=" + System.currentTimeMillis()  // 带时间戳
                    };
                    
                    // 返回第一个可能的URL
                    log.debug("尝试构建可能的图片URL: {}", possibleUrls[0]);
                    return possibleUrls[0];
                }
                
                // 如果URL包含 /l/?i=，提取参数
                if (originalUrl.contains("/l/?i="))
                {
                    int iParamIndex = originalUrl.indexOf("?i=");
                    if (iParamIndex > 0)
                    {
                        String baseUrl = originalUrl.substring(0, originalUrl.indexOf("/l/"));
                        String paramValue = originalUrl.substring(iParamIndex + 3);
                        int paramEnd = paramValue.indexOf("&");
                        if (paramEnd > 0)
                        {
                            paramValue = paramValue.substring(0, paramEnd);
                        }
                        paramValue = java.net.URLDecoder.decode(paramValue, "UTF-8");
                        
                        if (paramValue.startsWith("/"))
                        {
                            return baseUrl + paramValue;
                        }
                        else
                        {
                            return baseUrl + "/" + paramValue;
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.debug("提取真实图片URL失败: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 提取文件扩展名
     */
    private String extractFileExtension(String url)
    {
        if (url == null || url.isEmpty())
        {
            return "unknown";
        }
        
        // 移除查询参数和片段标识符
        String cleanUrl = url;
        int queryIndex = cleanUrl.indexOf('?');
        if (queryIndex > 0)
        {
            cleanUrl = cleanUrl.substring(0, queryIndex);
        }
        int fragmentIndex = cleanUrl.indexOf('#');
        if (fragmentIndex > 0)
        {
            cleanUrl = cleanUrl.substring(0, fragmentIndex);
        }
        
        int lastDot = cleanUrl.lastIndexOf('.');
        int lastSlash = cleanUrl.lastIndexOf('/');
        if (lastDot > lastSlash && lastDot < cleanUrl.length() - 1)
        {
            String extension = cleanUrl.substring(lastDot + 1).toLowerCase();
            // 移除可能的非法字符
            extension = extension.replaceAll("[^a-z0-9]", "");
            return extension.isEmpty() ? "unknown" : extension;
        }
        return "unknown";
    }
    
    /**
     * 清理URL，移除查询参数和片段标识符，用于创建文件名
     */
    private String cleanUrlForFilename(String url)
    {
        if (url == null || url.isEmpty())
        {
            return url;
        }
        
        // 移除查询参数
        int queryIndex = url.indexOf('?');
        if (queryIndex > 0)
        {
            url = url.substring(0, queryIndex);
        }
        
        // 移除片段标识符
        int fragmentIndex = url.indexOf('#');
        if (fragmentIndex > 0)
        {
            url = url.substring(0, fragmentIndex);
        }
        
        return url;
    }
    
    /**
     * 清理文件名，移除文件系统不允许的非法字符
     */
    private String sanitizeFileName(String fileName)
    {
        if (fileName == null || fileName.isEmpty())
        {
            return fileName;
        }
        
        // Windows/Linux文件系统不允许的字符: < > : " / \ | ? *
        // 同时移除控制字符
        String sanitized = fileName.replaceAll("[<>:\"/\\\\|?*\\x00-\\x1F]", "_");
        
        // 移除开头和结尾的点或空格（Windows不允许）
        sanitized = sanitized.replaceAll("^[\\.\\s]+", "").replaceAll("[\\.\\s]+$", "");
        
        // 确保文件名不为空
        if (sanitized.isEmpty())
        {
            sanitized = "image_" + System.currentTimeMillis();
        }
        
        // 限制文件名长度（Windows最大255字符）
        if (sanitized.length() > 255)
        {
            String extension = "";
            int lastDot = sanitized.lastIndexOf('.');
            if (lastDot > 0 && lastDot < sanitized.length() - 1)
            {
                extension = sanitized.substring(lastDot);
                sanitized = sanitized.substring(0, lastDot);
            }
            sanitized = sanitized.substring(0, 255 - extension.length()) + extension;
        }
        
        return sanitized;
    }
    
    /**
     * 更新任务状态
     */
    private void updateTaskStatus(Long taskId, String status, String errorMsg)
    {
        try
        {
            CrawlerTaskEntity task = taskService.getById(taskId);
            if (task != null)
            {
                task.setStatus(status);
                if (errorMsg != null)
                {
                    task.setErrorMsg(errorMsg);
                }
                if ("COMPLETED".equals(status) || "STOPPED".equals(status) || "ERROR".equals(status))
                {
                    task.setEndTime(new Date());
                }
                taskService.updateById(task);
                
                // SSE推送状态变化
                publishTaskStatus(task, status);
            }
        }
        catch (Exception e)
        {
            log.error("更新任务状态失败: {}", taskId, e);
        }
    }
    
    /**
     * 更新任务进度
     */
    private void updateTaskProgress(Long taskId, int totalUrls, int crawledUrls, 
                                    int successCount, int errorCount)
    {
        try
        {
            CrawlerTaskEntity task = taskService.getById(taskId);
            if (task != null)
            {
                task.setTotalUrls(totalUrls);
                task.setCrawledUrls(crawledUrls);
                task.setSuccessCount(successCount);
                task.setErrorCount(errorCount);
                taskService.updateById(task);
                
                // SSE推送进度更新
                publishTaskProgress(task);
            }
        }
        catch (Exception e)
        {
            log.error("更新任务进度失败: {}", taskId, e);
        }
    }
    
    /**
     * SSE推送任务状态变化
     */
    private void publishTaskStatus(CrawlerTaskEntity task, String status)
    {
        try
        {
            Map<String, Object> data = buildTaskStatusData(task);
            String eventName = switch (status) {
                case "RUNNING" -> "task.started";
                case "COMPLETED" -> "task.completed";
                case "STOPPED" -> "task.stopped";
                case "ERROR" -> "task.error";
                case "PAUSED" -> "task.paused";
                default -> "task.status";
            };
            ssePublisher.publish(SSE_TOPIC_TASK_STATUS, eventName, data);
        }
        catch (Exception e)
        {
            log.debug("SSE推送任务状态失败: {}", e.getMessage());
        }
    }
    
    /**
     * SSE推送任务进度更新
     */
    private void publishTaskProgress(CrawlerTaskEntity task)
    {
        try
        {
            Map<String, Object> data = buildTaskStatusData(task);
            ssePublisher.publish(SSE_TOPIC_TASK_STATUS, "task.progress", data);
        }
        catch (Exception e)
        {
            log.debug("SSE推送任务进度失败: {}", e.getMessage());
        }
    }
    
    /**
     * 构建任务状态数据
     */
    private Map<String, Object> buildTaskStatusData(CrawlerTaskEntity task)
    {
        Map<String, Object> data = new HashMap<>();
        data.put("id", task.getId());
        data.put("taskName", task.getTaskName());
        data.put("status", task.getStatus());
        data.put("totalUrls", task.getTotalUrls() != null ? task.getTotalUrls() : 0);
        data.put("crawledUrls", task.getCrawledUrls() != null ? task.getCrawledUrls() : 0);
        data.put("successCount", task.getSuccessCount() != null ? task.getSuccessCount() : 0);
        data.put("errorCount", task.getErrorCount() != null ? task.getErrorCount() : 0);
        data.put("startTime", task.getStartTime());
        data.put("endTime", task.getEndTime());
        data.put("errorMsg", task.getErrorMsg());
        
        // 计算进度百分比
        Integer totalUrls = task.getTotalUrls();
        Integer crawledUrls = task.getCrawledUrls();
        if (totalUrls != null && totalUrls > 0 && crawledUrls != null) {
            double progress = (double) crawledUrls / totalUrls * 100;
            data.put("progress", Math.round(progress * 100.0) / 100.0);
        } else {
            data.put("progress", 0.0);
        }
        
        return data;
    }
    
    /**
     * 添加日志
     */
    private void addLog(Long taskId, String level, String logType, String message)
    {
        try
        {
            CrawlerLogEntity logEntity = new CrawlerLogEntity();
            logEntity.setTaskId(taskId);
            logEntity.setLogType(logType);
            logEntity.setLevel(level);
            logEntity.setMessage(message);
            logEntity.setCreateTime(new Date());
            
            // 从任务获取权限信息
            CrawlerTaskEntity task = taskService.getById(taskId);
            if (task != null) {
                logEntity.setCreateBy(task.getCreateBy());
                logEntity.setDeptId(task.getDeptId());
            }
            
            logService.save(logEntity);
        }
        catch (Exception e)
        {
            log.error("添加日志失败: {}", taskId, e);
        }
    }
    
    /**
     * 停止任务
     */
    public void stopTask(Long taskId)
    {
        if (runningTasks.containsKey(taskId))
        {
            runningTasks.remove(taskId);
            log.info("任务 {} 已收到停止请求，将在下一个检查点停止", taskId);
            addLog(taskId, "INFO", "停止请求", "任务已收到停止请求，正在安全停止...");
        }
        else
        {
            log.warn("任务 {} 不在运行中，无需停止", taskId);
        }
    }
    
    /**
     * 确保任务不在运行状态（清理可能残留的运行状态）
     */
    public void ensureTaskNotRunning(Long taskId)
    {
        runningTasks.remove(taskId);
    }
    
    /**
     * 解析代理列表
     */
    private EffectiveProxyList parseProxyList(CrawlerTaskEntity task)
    {
        if (task.getUseProxy() == null || task.getUseProxy() != 1)
        {
            return new EffectiveProxyList("NONE", Collections.emptyList());
        }

        String taskProxyListJson = task.getProxyList();
        if (taskProxyListJson == null || taskProxyListJson.trim().isEmpty() || "[]".equals(taskProxyListJson.trim()))
        {
            return new EffectiveProxyList("GLOBAL", getEnabledProxyListFromDb());
        }

        try
        {
            // 解析代理列表（JSON格式：[{"type":"HTTP","host":"127.0.0.1","port":8080,"username":"","password":""}]）
            List<Map<String, Object>> proxyMapList = objectMapper.readValue(taskProxyListJson,
                    objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
            List<ProxyInfo> proxyList = new ArrayList<>();
            for (Map<String, Object> proxyMap : proxyMapList)
            {
                ProxyInfo proxyInfo = new ProxyInfo();
                proxyInfo.type = String.valueOf(proxyMap.getOrDefault("type", "HTTP"));
                proxyInfo.host = (String) proxyMap.get("host");
                proxyInfo.port = proxyMap.get("port") instanceof Integer 
                        ? (Integer) proxyMap.get("port") 
                        : Integer.parseInt(String.valueOf(proxyMap.get("port")));
                proxyInfo.username = (String) proxyMap.getOrDefault("username", "");
                proxyInfo.password = (String) proxyMap.getOrDefault("password", "");
                if (proxyInfo.host != null && !proxyInfo.host.isEmpty() && proxyInfo.port > 0)
                {
                    proxyList.add(proxyInfo);
                }
            }
            if (proxyList.isEmpty())
            {
                // 任务代理列表解析后为空：回退到全局代理
                return new EffectiveProxyList("GLOBAL", getEnabledProxyListFromDb());
            }
            return new EffectiveProxyList("TASK", proxyList);
        }
        catch (Exception e)
        {
            log.warn("解析代理列表失败: {}", e.getMessage());
            // 任务代理列表解析失败：回退到全局代理
            return new EffectiveProxyList("GLOBAL", getEnabledProxyListFromDb());
        }
    }

    private List<ProxyInfo> getEnabledProxyListFromDb()
    {
        return proxyService.listEnabled().stream()
                .map(p -> {
                    ProxyInfo pi = new ProxyInfo();
                    pi.type = p.getProxyType();
                    pi.host = p.getHost();
                    pi.port = p.getPort() != null ? p.getPort() : 0;
                    pi.username = p.getUsername();
                    pi.password = p.getPassword();
                    return pi;
                })
                .filter(pi -> pi.host != null && !pi.host.isEmpty() && pi.port > 0)
                .toList();
    }

    private static class EffectiveProxyList
    {
        String source; // TASK / GLOBAL / NONE
        List<ProxyInfo> proxies;

        EffectiveProxyList(String source, List<ProxyInfo> proxies)
        {
            this.source = source;
            this.proxies = proxies;
        }
    }
    
    /**
     * 代理信息
     */
    private static class ProxyInfo
    {
        String type; // HTTP / SOCKS
        String host;
        int port;
        String username;
        String password;
    }
    
    /**
     * URL信息
     */
    private static class UrlInfo
    {
        String url;
        int depth;
        
        UrlInfo(String url, int depth)
        {
            this.url = url;
            this.depth = depth;
        }
    }
}

