package com.mayday.crawler.service.impl;

import com.mayday.common.util.StringUtils;
import com.mayday.crawler.mapper.CrawlerArticleMapper;
import com.mayday.crawler.mapper.CrawlerImageMapper;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverDTO;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverQueryReq;
import com.mayday.crawler.modl.dto.CrawlerImageQueryReq;
import com.mayday.crawler.modl.entity.CrawlerArticleEntity;
import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mayday.crawler.modl.vo.CrawlerImageVo;
import com.mayday.crawler.service.ICrawlerImageService;
import com.mayday.crawler.util.CrawlerDataScopeUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mayday.crawler.modl.entity.table.CrawlerArticleEntityTableDef.CRAWLER_ARTICLE_ENTITY;
import static com.mayday.crawler.modl.entity.table.CrawlerImageEntityTableDef.CRAWLER_IMAGE_ENTITY;

/**
 * 图片服务实现
 * 
 * 使用 MyBatis-Flex QueryWrapper API 实现所有查询逻辑，
 * 保持类型安全和流畅的链式调用。
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CrawlerImageServiceImpl extends ServiceImpl<CrawlerImageMapper, CrawlerImageEntity> implements ICrawlerImageService {

    private final CrawlerArticleMapper articleMapper;

    /**
     * 图片存储根目录（兼容两种配置键）
     */
    @Value("${crawler.image.base-path:${crawler.image-base-path:./data/crawler-images}}")
    private String imageBasePath;

    @Override
    public Page<CrawlerImageEntity> queryList(CrawlerImageQueryReq req) {
        // 使用 MyBatis-Flex QueryWrapper 构建查询条件
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_IMAGE_ENTITY.ID.eq(req.getId()).when(req.getId() != null))
                .and(CRAWLER_IMAGE_ENTITY.TASK_ID.eq(req.getTaskId()).when(req.getTaskId() != null))
                .and(CRAWLER_IMAGE_ENTITY.ARTICLE_ID.eq(req.getArticleId()).when(req.getArticleId() != null))
                .and(CRAWLER_IMAGE_ENTITY.DOWNLOAD_STATUS.eq(req.getDownloadStatus()).when(StringUtils.isNotEmpty(req.getDownloadStatus())))
                .and(CRAWLER_IMAGE_ENTITY.FORMAT.eq(req.getFormat()).when(StringUtils.isNotEmpty(req.getFormat())))
                .orderBy(CRAWLER_IMAGE_ENTITY.CREATE_TIME, false);

        // 注：图片权限通过关联的文章表过滤，此处不需要直接过滤
        return page(new Page<>(req.getCurrent(), req.getPageSize()), wrapper);
    }

    @Override
    public Page<CrawlerImageArticleCoverDTO> queryArticleCoverPage(CrawlerImageArticleCoverQueryReq req) {
        long current = req.getCurrent() <= 0 ? 1 : req.getCurrent();
        long pageSize = req.getPageSize() <= 0 ? 10 : req.getPageSize();

        // 获取数据权限过滤参数
        String dataScope = CrawlerDataScopeUtil.getDataScope();
        Long createBy = null;
        Long deptId = null;
        if ("5".equals(dataScope)) {
            // 仅本人数据
            createBy = CrawlerDataScopeUtil.getCurrentUserId();
        } else if ("3".equals(dataScope) || "4".equals(dataScope)) {
            // 本部门或本部门及以下
            deptId = CrawlerDataScopeUtil.getCurrentDeptId();
        }

        // Step 1: 使用 MyBatis-Flex QueryWrapper 查询有图片的文章列表
        QueryWrapper articleWrapper = buildArticleQueryWrapper(req.getTaskId(), req.getTitle(), createBy, deptId);
        
        // 分页查询文章
        Page<CrawlerArticleEntity> articlePage = articleMapper.paginate(
                new Page<>(current, pageSize), 
                articleWrapper
        );

        if (articlePage.getRecords() == null || articlePage.getRecords().isEmpty()) {
            Page<CrawlerImageArticleCoverDTO> emptyPage = new Page<>(current, pageSize);
            emptyPage.setTotalRow(0);
            emptyPage.setRecords(new ArrayList<>());
            return emptyPage;
        }

        // Step 2: 批量获取这些文章的封面图片（每篇文章的第一张图片）
        List<Long> articleIds = articlePage.getRecords().stream()
                .map(CrawlerArticleEntity::getId)
                .collect(Collectors.toList());

        Map<Long, CrawlerImageEntity> coverMap = getCoverImagesForArticles(articleIds);

        // Step 3: 组装返回结果
        List<CrawlerImageArticleCoverDTO> records = articlePage.getRecords().stream()
                .map(article -> buildCoverDTO(article, coverMap.get(article.getId())))
                .collect(Collectors.toList());

        // 处理封面图片本地路径
        processCoverLocalPaths(records);

        Page<CrawlerImageArticleCoverDTO> page = new Page<>(current, pageSize);
        page.setTotalRow(articlePage.getTotalRow());
        page.setRecords(records);
        return page;
    }

    /**
     * 构建文章查询条件（只查询有图片的文章）
     * 使用 MyBatis-Flex QueryWrapper 实现类型安全的条件构建
     */
    private QueryWrapper buildArticleQueryWrapper(Long taskId, String title, Long createBy, Long deptId) {
        return QueryWrapper.create()
                .from(CRAWLER_ARTICLE_ENTITY)
                .where(CRAWLER_ARTICLE_ENTITY.TASK_ID.eq(taskId).when(taskId != null))
                .and(CRAWLER_ARTICLE_ENTITY.TITLE.like(title).when(StringUtils.isNotEmpty(title)))
                .and(CRAWLER_ARTICLE_ENTITY.CREATE_BY.eq(createBy).when(createBy != null))
                .and(CRAWLER_ARTICLE_ENTITY.DEPT_ID.eq(deptId).when(deptId != null))
                // 使用 exists 方法正确构建子查询
                .and("EXISTS (SELECT 1 FROM crawler_image WHERE crawler_image.article_id = crawler_article.id)")
                .orderBy(CRAWLER_ARTICLE_ENTITY.CREATE_TIME, false);
    }

    /**
     * 批量获取文章的封面图片（每篇文章的第一张图片）
     * 使用 MyBatis-Flex 流畅API分组查询
     */
    private Map<Long, CrawlerImageEntity> getCoverImagesForArticles(List<Long> articleIds) {
        if (articleIds == null || articleIds.isEmpty()) {
            return Map.of();
        }

        // 查询所有相关图片
        QueryWrapper wrapper = QueryWrapper.create()
                .from(CRAWLER_IMAGE_ENTITY)
                .where(CRAWLER_IMAGE_ENTITY.ARTICLE_ID.in(articleIds))
                .orderBy(CRAWLER_IMAGE_ENTITY.ID, true);

        List<CrawlerImageEntity> allImages = list(wrapper);

        // 按文章ID分组，取每组的第一张图片作为封面
        return allImages.stream()
                .collect(Collectors.groupingBy(
                        CrawlerImageEntity::getArticleId,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.isEmpty() ? null : list.get(0)
                        )
                ));
    }

    /**
     * 构建封面DTO
     */
    private CrawlerImageArticleCoverDTO buildCoverDTO(CrawlerArticleEntity article, CrawlerImageEntity coverImage) {
        CrawlerImageArticleCoverDTO dto = new CrawlerImageArticleCoverDTO();
        dto.setArticleId(article.getId());
        dto.setTaskId(article.getTaskId());
        dto.setArticleTitle(article.getTitle());
        dto.setArticleUrl(article.getUrl());
        dto.setSourceSite(article.getSourceSite());
        dto.setPublishTime(article.getPublishTime());
        dto.setArticleCreateTime(article.getCreateTime());

        if (coverImage != null) {
            dto.setCoverUrl(coverImage.getUrl());
            dto.setCoverFilePath(coverImage.getFilePath());
            dto.setCoverFileName(coverImage.getFileName());
            dto.setCoverDownloadStatus(coverImage.getDownloadStatus());
        }

        // 统计该文章下的图片数量
        dto.setImageCount(countImagesByArticleId(article.getId()));
        return dto;
    }

    /**
     * 统计文章下的图片数量
     */
    private Long countImagesByArticleId(Long articleId) {
        if (articleId == null) {
            return 0L;
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .from(CRAWLER_IMAGE_ENTITY)
                .where(CRAWLER_IMAGE_ENTITY.ARTICLE_ID.eq(articleId));
        return count(wrapper);
    }

    /**
     * 处理封面图片本地路径
     * coverFilePath 是目录路径，coverFileName 是文件名，需要拼接生成完整的本地访问URL
     */
    private void processCoverLocalPaths(List<CrawlerImageArticleCoverDTO> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        Path basePath = Paths.get(imageBasePath).toAbsolutePath().normalize();
        for (CrawlerImageArticleCoverDTO dto : records) {
            if ("SUCCESS".equals(dto.getCoverDownloadStatus()) 
                    && dto.getCoverFilePath() != null 
                    && dto.getCoverFileName() != null) {
                try {
                    // coverFilePath 是目录路径
                    Path dirPath = Paths.get(dto.getCoverFilePath()).toAbsolutePath().normalize();
                    // 构建完整文件路径
                    Path fullFilePath = dirPath.resolve(dto.getCoverFileName()).normalize();
                    
                    String displayUrl;
                    if (fullFilePath.startsWith(basePath)) {
                        // 计算相对于基础路径的相对路径
                        String relativePath = basePath.relativize(fullFilePath).toString().replace("\\", "/");
                        displayUrl = "/crawler-images/" + relativePath;
                    } else {
                        // 路径不在基础目录内，使用文件名
                        displayUrl = "/crawler-images/" + dto.getCoverFileName();
                    }
                    dto.setCoverUrl(displayUrl);
                    log.debug("[COVER] 封面图片本地URL: articleId={}, displayUrl={}", dto.getArticleId(), displayUrl);
                } catch (Exception e) {
                    log.warn("[COVER] 处理封面路径失败: articleId={}, filePath={}, fileName={}, error={}", 
                            dto.getArticleId(), dto.getCoverFilePath(), dto.getCoverFileName(), e.getMessage());
                }
            }
        }
    }

    @Override
    public List<CrawlerImageEntity> listByArticleId(Long articleId) {
        if (articleId == null) {
            return List.of();
        }
        // 使用 MyBatis-Flex QueryWrapper 替代原来的 @Select 方法
        QueryWrapper wrapper = QueryWrapper.create()
                .from(CRAWLER_IMAGE_ENTITY)
                .where(CRAWLER_IMAGE_ENTITY.ARTICLE_ID.eq(articleId))
                .orderBy(CRAWLER_IMAGE_ENTITY.ID, true);
        return list(wrapper);
    }

    @Override
    public boolean removeImageWithFileById(Long id) {
        if (id == null) {
            return false;
        }
        CrawlerImageEntity entity = getById(id);
        safeDeleteLocalFile(entity);
        return removeById(id);
    }

    @Override
    public boolean removeImagesWithFileByArticleId(Long articleId) {
        if (articleId == null) {
            return false;
        }
        List<CrawlerImageEntity> list = listByArticleId(articleId);
        if (list != null) {
            for (CrawlerImageEntity entity : list) {
                safeDeleteLocalFile(entity);
            }
            safeCleanupEmptyDirs(list);
        }
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_IMAGE_ENTITY.ARTICLE_ID.eq(articleId));
        return remove(wrapper);
    }

    /**
     * 安全删除本地图片文件
     */
    private void safeDeleteLocalFile(CrawlerImageEntity entity) {
        if (entity == null) {
            return;
        }
        String fp = entity.getFilePath();
        String fn = entity.getFileName();
        if (fp == null || fp.isBlank()) {
            return;
        }

        try {
            Path baseDir = Paths.get(imageBasePath).toAbsolutePath().normalize();
            Path raw = Paths.get(fp).toAbsolutePath().normalize();

            Path candidate = raw;
            if (fn != null && !fn.isBlank()) {
                if (Files.isDirectory(raw) || !raw.getFileName().toString().equals(fn)) {
                    candidate = raw.resolve(fn).toAbsolutePath().normalize();
                }
            }

            if (!candidate.startsWith(baseDir)) {
                log.warn("[SAFE-DELETE] 拒绝删除越界路径: candidate={}, baseDir={}, imageId={}, articleId={}",
                        candidate, baseDir, entity.getId(), entity.getArticleId());
                return;
            }

            if (Files.exists(candidate) && !Files.isRegularFile(candidate)) {
                log.warn("[SAFE-DELETE] 拒绝删除非普通文件: {}", candidate);
                return;
            }

            boolean deleted = Files.deleteIfExists(candidate);
            if (deleted) {
                log.info("[SAFE-DELETE] 已删除图片文件: {}", candidate);
            }
        } catch (Exception e) {
            log.warn("[SAFE-DELETE] 删除图片文件失败: filePath={}, fileName={}, err={}",
                    fp, fn, e.getMessage());
        }
    }

    /**
     * 清理空目录（仅限 imageBasePath 下）
     */
    private void safeCleanupEmptyDirs(List<CrawlerImageEntity> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        try {
            Path baseDir = Paths.get(imageBasePath).toAbsolutePath().normalize();
            for (CrawlerImageEntity entity : list) {
                String fp = entity.getFilePath();
                if (fp == null || fp.isBlank()) {
                    continue;
                }
                Path dir = Paths.get(fp).toAbsolutePath().normalize();
                if (!dir.startsWith(baseDir)) {
                    continue;
                }
                if (Files.exists(dir) && Files.isDirectory(dir)) {
                    try {
                        Files.deleteIfExists(dir);
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (Exception e) {
            log.debug("[SAFE-DELETE] 清理空目录失败: {}", e.getMessage());
        }
    }

    /**
     * 将Entity转换为VO,并设置displayUrl(优先使用本地路径)
     */
    public CrawlerImageVo convertToVo(CrawlerImageEntity entity) {
        if (entity == null) {
            return null;
        }
        CrawlerImageVo vo = new CrawlerImageVo();
        BeanUtils.copyProperties(entity, vo);

        if ("SUCCESS".equals(entity.getDownloadStatus())
                && entity.getFilePath() != null
                && entity.getFileName() != null) {
            try {
                Path basePath = Paths.get(imageBasePath).toAbsolutePath().normalize();
                // filePath 是目录路径
                Path dirPath = Paths.get(entity.getFilePath()).toAbsolutePath().normalize();
                // 构建完整文件路径
                Path fullFilePath = dirPath.resolve(entity.getFileName()).normalize();

                String displayUrl;
                if (fullFilePath.startsWith(basePath)) {
                    String relativePath = basePath.relativize(fullFilePath).toString().replace("\\", "/");
                    displayUrl = "/crawler-images/" + relativePath;
                } else {
                    displayUrl = "/crawler-images/" + entity.getFileName();
                }
                vo.setDisplayUrl(displayUrl);
            } catch (Exception e) {
                log.warn("解析图片路径失败,使用原始URL: filePath={}, fileName={}, error={}",
                        entity.getFilePath(), entity.getFileName(), e.getMessage());
                vo.setDisplayUrl(entity.getUrl());
            }
        } else {
            vo.setDisplayUrl(entity.getUrl());
        }

        return vo;
    }

    /**
     * 批量转换Entity列表为VO列表
     */
    public List<CrawlerImageVo> convertToVoList(List<CrawlerImageEntity> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::convertToVo)
                .collect(Collectors.toList());
    }
}
