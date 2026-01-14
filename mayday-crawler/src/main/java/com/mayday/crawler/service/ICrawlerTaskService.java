package com.mayday.crawler.service;

import com.mayday.crawler.modl.entity.CrawlerTaskEntity;
import com.mayday.crawler.modl.dto.CrawlerTaskEditReq;
import com.mayday.crawler.modl.dto.CrawlerTaskQueryReq;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

import java.util.List;
import java.util.Map;

/**
 * 爬虫任务服务接口
 *
 * @author Antigravity
 * @since 1.0.0
 */
public interface ICrawlerTaskService extends IService<CrawlerTaskEntity> {

    /**
     * 分页查询任务列表
     *
     * @param req 查询请求
     * @return 分页结果
     */
    Page<CrawlerTaskEntity> queryList(CrawlerTaskQueryReq req);

    /**
     * 新增或更新任务
     *
     * @param req 请求参数
     * @return 任务ID
     */
    Long saveOrUpdateTask(CrawlerTaskEditReq req);

    /**
     * 删除任务
     *
     * @param id 任务ID
     * @return 是否删除成功
     */
    boolean removeTask(Long id);

    /**
     * 启动任务
     *
     * @param id 任务ID
     * @return 是否启动成功
     */
    boolean startTask(Long id);

    /**
     * 暂停任务
     *
     * @param id 任务ID
     * @return 是否暂停成功
     */
    boolean pauseTask(Long id);

    /**
     * 恢复任务
     *
     * @param id 任务ID
     * @return 是否恢复成功
     */
    boolean resumeTask(Long id);

    /**
     * 停止任务
     *
     * @param id 任务ID
     * @return 是否停止成功
     */
    boolean stopTask(Long id);

    /**
     * 重跑任务
     *
     * @param id 任务ID
     * @return 是否重跑成功
     */
    boolean rerunTask(Long id);

    /**
     * 查询任务运行状态
     *
     * @param id 任务ID
     * @return 任务状态信息
     */
    Map<String, Object> getTaskStatus(Long id);

    /**
     * 查询所有运行中的任务
     *
     * @return 运行中的任务列表
     */
    List<Map<String, Object>> getRunningTasks();
}
