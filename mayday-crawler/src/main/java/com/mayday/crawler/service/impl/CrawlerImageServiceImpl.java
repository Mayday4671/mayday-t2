package com.mayday.crawler.service.impl;

import com.mayday.common.util.StringUtils;
import com.mayday.crawler.mapper.CrawlerImageMapper;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverDTO;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverQueryReq;
import com.mayday.crawler.modl.dto.CrawlerImageQueryReq;
import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mayday.crawler.modl.vo.CrawlerImageVo;
import com.mayday.crawler.service.ICrawlerImageService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import static com.mayday.crawler.modl.entity.table.CrawlerImageEntityTableDef.CRAWLER_IMAGE_ENTITY;

import com.mayday.crawler.util.CrawlerDataScopeUtil;

/**
 * 图片服务实现
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Service
@Slf4j
public class CrawlerImageServiceImpl extends ServiceImpl<CrawlerImageMapper, CrawlerImageEntity> implements ICrawlerImageService {

    /**
     * 图片存储根目录（兼容两种配置键）
     */
    @Value("${crawler.image.base-path:${crawler.image-base-path:./data/crawler-images}}")
    private String imageBasePath;

    @Override
    public Page<CrawlerImageEntity> queryList(CrawlerImageQueryReq req) {
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
        long offset = (current - 1) * pageSize;

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
        // dataScope=1 (全部) 或 dataScope=2 (自定义) 时，createBy和deptId都为null，不过滤

        List<CrawlerImageArticleCoverDTO> records =
                mapper.selectArticleCoverPage(offset, pageSize, req.getTaskId(), req.getTitle(), createBy, deptId);
        
        // 处理封面图片本地路径
        if (records != null && !records.isEmpty()) {
            Path basePath = Paths.get(imageBasePath).toAbsolutePath().normalize();
            for (CrawlerImageArticleCoverDTO dto : records) {
                if ("SUCCESS".equals(dto.getCoverDownloadStatus()) 
                        && dto.getCoverFilePath() != null 
                        && dto.getCoverFileName() != null) {
                    try {
                        Path filePath = Paths.get(dto.getCoverFilePath()).toAbsolutePath().normalize();
                        String relativePath;
                        if (filePath.startsWith(basePath)) {
                            relativePath = basePath.relativize(filePath).toString().replace("\\", "/");
                        } else {
                            relativePath = filePath.getFileName().toString();
                        }
                        String displayUrl = "/crawler-images/" + relativePath.replace("\\", "/") + (relativePath.endsWith(dto.getCoverFileName()) ? "" : "/" + dto.getCoverFileName());
                        dto.setCoverUrl(displayUrl);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
        }

        Long total = mapper.countArticleCover(req.getTaskId(), req.getTitle(), createBy, deptId);

        Page<CrawlerImageArticleCoverDTO> page = new Page<>(current, pageSize);
        page.setTotalRow(total == null ? 0 : total);
        page.setRecords(records);
        return page;
    }

    @Override
    public List<CrawlerImageEntity> listByArticleId(Long articleId) {
        if (articleId == null) {
            return List.of();
        }
        return mapper.selectByArticleId(articleId);
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
                Path filePath = Paths.get(entity.getFilePath()).toAbsolutePath().normalize();

                String relativePath;
                if (filePath.startsWith(basePath)) {
                    relativePath = basePath.relativize(filePath).toString().replace("\\", "/");
                } else {
                    relativePath = filePath.getFileName().toString();
                }

                vo.setDisplayUrl("/crawler-images/" + relativePath.replace("\\", "/") + (relativePath.endsWith(entity.getFileName()) ? "" : "/" + entity.getFileName()));
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
