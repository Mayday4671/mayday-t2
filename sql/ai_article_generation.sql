-- AI 文章生成功能 - 数据库变更

-- 1. 为 crawler_article 表添加审核状态和来源类型字段
ALTER TABLE crawler_article ADD COLUMN status INT DEFAULT 0 COMMENT '审核状态：0-待审核 1-已发布 2-已驳回';
ALTER TABLE crawler_article ADD COLUMN source_type VARCHAR(20) DEFAULT 'CRAWLER' COMMENT '来源类型：CRAWLER-爬虫采集 AI-AI生成 MANUAL-手动录入';

-- 2. 插入 AI 文章生成场景配置（需要先在 ai_key 表中添加密钥）
-- 请根据实际的 key_id 修改下面的语句
-- INSERT INTO ai_config (scene_code, provider, model_name, base_url, key_id, temperature, max_tokens, enabled, priority, weight, version)
-- VALUES ('article_generation', 'google', 'gemini-1.5-flash', NULL, {your_key_id}, 0.7, 4096, 1, 1, 100, 1);

-- 3. 添加 AI 文章生成权限菜单
INSERT INTO sys_menu (menu_name, parent_id, perms, menu_type, visible, status)
VALUES ('AI生成文章', (SELECT id FROM sys_menu WHERE menu_name = '文章管理' LIMIT 1), 'article:ai:generate', 'F', '0', '0');

-- 4. 将权限分配给超级管理员
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE perms = 'article:ai:generate';
