-- ================================================
-- 爬虫模块数据权限字段迁移脚本
-- 作者: Antigravity
-- 日期: 2026-01-14
-- ================================================

-- 1. 为 crawler_task 表添加权限字段
ALTER TABLE crawler_task
  ADD COLUMN create_by BIGINT NULL COMMENT '创建人ID' AFTER update_time,
  ADD COLUMN dept_id BIGINT NULL COMMENT '所属部门ID' AFTER create_by,
  ADD INDEX idx_crawler_task_create_by (create_by),
  ADD INDEX idx_crawler_task_dept_id (dept_id);

-- 2. 为 crawler_article 表添加权限字段
ALTER TABLE crawler_article
  ADD COLUMN create_by BIGINT NULL COMMENT '创建人ID' AFTER update_time,
  ADD COLUMN dept_id BIGINT NULL COMMENT '所属部门ID' AFTER create_by,
  ADD INDEX idx_crawler_article_create_by (create_by),
  ADD INDEX idx_crawler_article_dept_id (dept_id);

-- 3. 为 crawler_log 表添加权限字段（图片表不需要，通过文章表关联过滤）
ALTER TABLE crawler_log
  ADD COLUMN create_by BIGINT NULL COMMENT '创建人ID' AFTER create_time,
  ADD COLUMN dept_id BIGINT NULL COMMENT '所属部门ID' AFTER create_by,
  ADD INDEX idx_crawler_log_create_by (create_by),
  ADD INDEX idx_crawler_log_dept_id (dept_id);

-- 4. 更新现有数据（可选：将现有数据归属到管理员）
-- UPDATE crawler_task SET create_by = 1, dept_id = 1 WHERE create_by IS NULL;
-- UPDATE crawler_article SET create_by = 1, dept_id = 1 WHERE create_by IS NULL;
-- UPDATE crawler_log SET create_by = 1, dept_id = 1 WHERE create_by IS NULL;
