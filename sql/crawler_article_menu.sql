-- ============================================
-- 文章管理模块权限配置
-- @author Antigravity
-- @since 2026-01-21
-- ============================================

-- 1. 内容管理（一级目录）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES ('内容管理', 0, 5, 'content', NULL, 1, 0, 'M', '0', '0', NULL, 'documentation', NOW(), NOW());

SET @contentParentId = LAST_INSERT_ID();

-- 2. 文章列表（二级菜单）
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES ('文章列表', @contentParentId, 1, 'article', 'admin/crawler/article/index', 1, 0, 'C', '0', '0', 'crawler:article:list', 'form', NOW(), NOW());

SET @articleMenuId = LAST_INSERT_ID();

-- 3. 文章按钮权限（三级功能点）
INSERT INTO `sys_menu` (`parent_id`, `menu_name`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`) VALUES
(@articleMenuId, '文章详情', 1, NULL, NULL, 1, 0, 'F', '0', '0', 'crawler:article:detail', '#', NOW(), NOW()),
(@articleMenuId, '文章删除', 2, NULL, NULL, 1, 0, 'F', '0', '0', 'crawler:article:remove', '#', NOW(), NOW()),
(@articleMenuId, '文章编辑', 3, NULL, NULL, 1, 0, 'F', '0', '0', 'crawler:article:edit', '#', NOW(), NOW()),
(@articleMenuId, '文章审核', 4, NULL, NULL, 1, 0, 'F', '0', '0', 'crawler:article:audit', '#', NOW(), NOW());

-- 4. 将新权限分配给超级管理员角色 (role_id = 1)
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, menu_id FROM sys_menu WHERE perms LIKE 'crawler:article:%' OR menu_name = '内容管理';
