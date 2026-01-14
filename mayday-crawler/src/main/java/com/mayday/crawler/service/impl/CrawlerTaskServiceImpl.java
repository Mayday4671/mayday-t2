package com.mayday.crawler.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mayday.common.enums.ErrorCode;
import com.mayday.common.exception.BusinessException;
import com.mayday.common.util.BeanConverterUtils;
import com.mayday.common.util.StringUtils;
import com.mayday.crawler.executor.CrawlerExecutor;
import com.mayday.crawler.mapper.CrawlerTaskMapper;
import com.mayday.crawler.modl.dto.CrawlerTaskEditReq;
import com.mayday.crawler.modl.dto.CrawlerTaskQueryReq;
import com.mayday.crawler.modl.entity.CrawlerTaskEntity;
import com.mayday.crawler.service.ICrawlerTaskService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.mayday.crawler.modl.entity.table.CrawlerTaskEntityTableDef.CRAWLER_TASK_ENTITY;

import com.mayday.crawler.util.CrawlerDataScopeUtil;

/**
 * 爬虫任务服务实现
 *
 * @author Antigravity
 * @since 1.0.0
 */
@Service
public class CrawlerTaskServiceImpl extends ServiceImpl<CrawlerTaskMapper, CrawlerTaskEntity> implements ICrawlerTaskService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CrawlerExecutor crawlerExecutor;

    public CrawlerTaskServiceImpl(@Lazy CrawlerExecutor crawlerExecutor) {
        this.crawlerExecutor = crawlerExecutor;
    }

    @Override
    public Page<CrawlerTaskEntity> queryList(CrawlerTaskQueryReq req) {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_TASK_ENTITY.ID.eq(req.getId()).when(req.getId() != null))
                .and(CRAWLER_TASK_ENTITY.TASK_NAME.like(req.getTaskName()).when(StringUtils.isNotEmpty(req.getTaskName())))
                .and(CRAWLER_TASK_ENTITY.STATUS.eq(req.getStatus()).when(StringUtils.isNotEmpty(req.getStatus())))
                .and(CRAWLER_TASK_ENTITY.CRAWL_TYPE.eq(req.getCrawlType()).when(StringUtils.isNotEmpty(req.getCrawlType())))
                .orderBy(CRAWLER_TASK_ENTITY.CREATE_TIME, false);

        // 应用数据权限过滤
        CrawlerDataScopeUtil.applyDataScope(wrapper, CRAWLER_TASK_ENTITY.CREATE_BY, CRAWLER_TASK_ENTITY.DEPT_ID);

        return page(new Page<>(req.getCurrent(), req.getPageSize()), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateTask(CrawlerTaskEditReq req) {
        CrawlerTaskEntity entity = BeanConverterUtils.convert(req, CrawlerTaskEntity.class);

        // 将起始URL列表转换为JSON字符串
        if (req.getStartUrls() != null && !req.getStartUrls().isEmpty()) {
            try {
                entity.setStartUrls(objectMapper.writeValueAsString(req.getStartUrls()));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "起始URL列表格式错误");
            }
        }

        // 确保图片选择器字段被正确设置
        if (req.getContentSelector() != null) {
            entity.setContentSelector(req.getContentSelector());
        } else {
            entity.setContentSelector(null);
        }

        if (req.getImageSelector() != null) {
            entity.setImageSelector(req.getImageSelector());
        } else {
            entity.setImageSelector(null);
        }

        if (req.getExcludeSelector() != null) {
            entity.setExcludeSelector(req.getExcludeSelector());
        } else {
            entity.setExcludeSelector(null);
        }

        if (req.getId() == null) {
            // 新增任务，默认状态为未启动
            entity.setStatus("NOT_STARTED");
            // 填充创建人和部门信息
            entity.setCreateBy(CrawlerDataScopeUtil.getCurrentUserId());
            entity.setDeptId(CrawlerDataScopeUtil.getCurrentDeptId());
            entity.setTotalUrls(0);
            entity.setCrawledUrls(0);
            entity.setSuccessCount(0);
            entity.setErrorCount(0);
            if (entity.getListMaxPages() == null || entity.getListMaxPages() < 1) {
                entity.setListMaxPages(1);
            }
            save(entity);
            return entity.getId();
        } else {
            // 更新任务
            CrawlerTaskEntity existing = getById(req.getId());
            if (existing == null) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
            }
            // 如果任务正在运行，不允许修改某些字段
            if ("RUNNING".equals(existing.getStatus())) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务运行中，不允许修改");
            }
            updateById(entity);
            return entity.getId();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeTask(Long id) {
        CrawlerTaskEntity entity = getById(id);
        if (entity == null) {
            return false;
        }
        if ("RUNNING".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务运行中，不允许删除");
        }
        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean startTask(Long id) {
        CrawlerTaskEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
        }

        String status = entity.getStatus();
        if (!"NOT_STARTED".equals(status) && !"STOPPED".equals(status) &&
                !"ERROR".equals(status) && !"COMPLETED".equals(status)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务状态不允许启动，当前状态：" + status);
        }

        if ("RUNNING".equals(status) || "PAUSED".equals(status)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "任务正在运行中，无法重复启动");
        }

        entity.setStatus("RUNNING");
        entity.setStartTime(new Date());
        entity.setErrorMsg("");
        boolean updated = updateById(entity);

        if (updated) {
            crawlerExecutor.ensureTaskNotRunning(id);
            crawlerExecutor.executeTask(id);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean pauseTask(Long id) {
        CrawlerTaskEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
        }
        if (!"RUNNING".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有运行中的任务才能暂停");
        }
        entity.setStatus("PAUSED");
        return updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean resumeTask(Long id) {
        CrawlerTaskEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
        }
        if (!"PAUSED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有已暂停的任务才能恢复");
        }
        entity.setStatus("RUNNING");
        return updateById(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean stopTask(Long id) {
        CrawlerTaskEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
        }
        if (!"RUNNING".equals(entity.getStatus()) && !"PAUSED".equals(entity.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "只有运行中或已暂停的任务才能停止");
        }
        entity.setStatus("STOPPED");
        entity.setEndTime(new Date());
        boolean updated = updateById(entity);

        if (updated) {
            crawlerExecutor.stopTask(id);
        }

        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean rerunTask(Long id) {
        CrawlerTaskEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
        }
        entity.setStatus("NOT_STARTED");
        entity.setCrawledUrls(0);
        entity.setSuccessCount(0);
        entity.setErrorCount(0);
        entity.setStartTime(null);
        entity.setEndTime(null);
        entity.setErrorMsg("");
        return updateById(entity);
    }

    @Override
    public Map<String, Object> getTaskStatus(Long id) {
        CrawlerTaskEntity entity = getById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务不存在");
        }

        Map<String, Object> status = new HashMap<>();
        status.put("id", entity.getId());
        status.put("taskName", entity.getTaskName());
        status.put("status", entity.getStatus());
        status.put("isRunning", "RUNNING".equals(entity.getStatus()));
        status.put("totalUrls", entity.getTotalUrls() != null ? entity.getTotalUrls() : 0);
        status.put("crawledUrls", entity.getCrawledUrls() != null ? entity.getCrawledUrls() : 0);
        status.put("successCount", entity.getSuccessCount() != null ? entity.getSuccessCount() : 0);
        status.put("errorCount", entity.getErrorCount() != null ? entity.getErrorCount() : 0);
        status.put("startTime", entity.getStartTime());
        status.put("endTime", entity.getEndTime());
        status.put("errorMsg", entity.getErrorMsg());

        Integer totalUrls = entity.getTotalUrls();
        Integer crawledUrls = entity.getCrawledUrls();
        if (totalUrls != null && totalUrls > 0 && crawledUrls != null) {
            double progress = (double) crawledUrls / totalUrls * 100;
            status.put("progress", Math.round(progress * 100.0) / 100.0);
        } else {
            status.put("progress", 0.0);
        }

        if (entity.getStartTime() != null) {
            long duration;
            if (entity.getEndTime() != null) {
                duration = (entity.getEndTime().getTime() - entity.getStartTime().getTime()) / 1000;
            } else if ("RUNNING".equals(entity.getStatus())) {
                duration = (System.currentTimeMillis() - entity.getStartTime().getTime()) / 1000;
            } else {
                duration = 0;
            }
            status.put("duration", duration);
        } else {
            status.put("duration", 0);
        }

        return status;
    }

    @Override
    public List<Map<String, Object>> getRunningTasks() {
        QueryWrapper wrapper = QueryWrapper.create()
                .where(CRAWLER_TASK_ENTITY.STATUS.eq("RUNNING"))
                .orderBy(CRAWLER_TASK_ENTITY.START_TIME, false);
        List<CrawlerTaskEntity> runningTasks = list(wrapper);

        return runningTasks.stream()
                .map(entity -> {
                    Map<String, Object> taskInfo = new HashMap<>();
                    taskInfo.put("id", entity.getId());
                    taskInfo.put("taskName", entity.getTaskName());
                    taskInfo.put("status", entity.getStatus());
                    taskInfo.put("crawledUrls", entity.getCrawledUrls() != null ? entity.getCrawledUrls() : 0);
                    taskInfo.put("totalUrls", entity.getTotalUrls() != null ? entity.getTotalUrls() : 0);
                    taskInfo.put("startTime", entity.getStartTime());

                    Integer totalUrls = entity.getTotalUrls();
                    Integer crawledUrls = entity.getCrawledUrls();
                    if (totalUrls != null && totalUrls > 0 && crawledUrls != null) {
                        double progress = (double) crawledUrls / totalUrls * 100;
                        taskInfo.put("progress", Math.round(progress * 100.0) / 100.0);
                    } else {
                        taskInfo.put("progress", 0.0);
                    }

                    if (entity.getStartTime() != null) {
                        long duration = (System.currentTimeMillis() - entity.getStartTime().getTime()) / 1000;
                        taskInfo.put("duration", duration);
                    }

                    return taskInfo;
                })
                .collect(Collectors.toList());
    }
}
