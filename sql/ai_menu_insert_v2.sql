-- AI 菜单脚本
-- 1. AI 配置管理（一级目录）
INSERT INTO `sys_menu` (`menu_name`, `path`, `component`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES ('AI 配置管理', 'ai-config-manage', NULL, '0', '0', NULL, 'robot', SYSDATE(), SYSDATE());

-- 获取刚才插入的一级菜单ID (假设 AI 配置管理 是最后一个插入的目录，实际环境请根据名字查询)
SET @parentId = LAST_INSERT_ID();

-- 2. 密钥管理
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `path`, `component`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES (@parentId, '密钥管理', 'ai-key', 'admin/ai/AiKey', '0', '0', 'ai:key:list', 'key', SYSDATE(), SYSDATE());

-- 密钥按钮权限
SET @keyId = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `perms`, `menu_type`, `visible`, `status`) VALUES
(@keyId, '密钥新增', 'ai:key:add', 'F', '0', '0'),
(@keyId, '密钥修改', 'ai:key:edit', 'F', '0', '0'),
(@keyId, '密钥查询', 'ai:key:query', 'F', '0', '0'),
(@keyId, '密钥删除', 'ai:key:remove', 'F', '0', '0');

-- 3. 路由配置
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `path`, `component`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES (@parentId, '路由配置', 'ai-config', 'admin/ai/AiConfig', '0', '0', 'ai:config:list', 'deployment-unit', SYSDATE(), SYSDATE());

-- 路由配置按钮权限
SET @configId = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `perms`, `menu_type`, `visible`, `status`) VALUES
(@configId, '配置新增', 'ai:config:add', 'F', '0', '0'),
(@configId, '配置修改', 'ai:config:edit', 'F', '0', '0'),
(@configId, '配置查询', 'ai:config:query', 'F', '0', '0'),
(@configId, '配置删除', 'ai:config:remove', 'F', '0', '0');

-- 4. 调用日志
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `path`, `component`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES (@parentId, '调用日志', 'ai-log', 'admin/ai/AiCallLog', '0', '0', 'ai:log:list', 'file-text', SYSDATE(), SYSDATE());

-- 日志按钮权限
SET @logId = LAST_INSERT_ID();
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `perms`, `menu_type`, `visible`, `status`) VALUES
(@logId, '日志查询', 'ai:log:query', 'F', '0', '0');

-- ============================================
-- 5. 将 AI 菜单权限分配给超级管理员角色 (role_id = 1)
-- ============================================
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE perms LIKE 'ai:%' OR menu_name = 'AI 配置管理';
