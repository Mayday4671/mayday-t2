package com.mayday.crawler.service;

import com.mayday.crawler.modl.entity.CrawlerImageEntity;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverDTO;
import com.mayday.crawler.modl.dto.CrawlerImageArticleCoverQueryReq;
import com.mayday.crawler.modl.dto.CrawlerImageQueryReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 图片服务接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
public interface ICrawlerImageService extends IService<CrawlerImageEntity> {

    /**
     * 分页查询图片列表
     *
     * @param req 查询请求
     * @return 分页结果
     */
    Page<CrawlerImageEntity> queryList(CrawlerImageQueryReq req);

    /**
     * 按文章聚合分页（一个文章一条记录，含封面与图片数量）
     *
     * @param req 查询请求
     * @return 分页结果
     */
    Page<CrawlerImageArticleCoverDTO> queryArticleCoverPage(CrawlerImageArticleCoverQueryReq req);

    /**
     * 查询文章下的所有图片（用于轮播）
     *
     * @param articleId 文章ID
     * @return 图片列表
     */
    List<CrawlerImageEntity> listByArticleId(Long articleId);

    /**
     * 删除单张图片记录，并尝试删除对应的本地文件
     *
     * @param id 图片ID
     * @return 是否删除成功
     */
    boolean removeImageWithFileById(Long id);

    /**
     * 按文章删除图片记录，并尝试删除对应的本地文件
     *
     * @param articleId 文章ID
     * @return 是否删除成功
     */
    boolean removeImagesWithFileByArticleId(Long articleId);
}
