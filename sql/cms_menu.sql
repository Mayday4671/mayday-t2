-- 插入内容管理一级菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES ('内容管理', 0, 50, 'cms', NULL, 1, 0, 'M', '0', '0', NULL, 'AppstoreOutlined', NOW(), NOW());

-- 获取刚插入的菜单ID
SET @parent_id = LAST_INSERT_ID();

-- 插入分类管理菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES ('分类管理', @parent_id, 1, 'category', 'admin/cms/category/index', 1, 0, 'C', '0', '0', 'cms:category:list', 'TagsOutlined', NOW(), NOW());

-- 插入菜单管理菜单
INSERT INTO `sys_menu` (`menu_name`, `parent_id`, `order_num`, `path`, `component`, `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, `perms`, `icon`, `create_time`, `update_time`)
VALUES ('菜单管理', @parent_id, 2, 'menu', 'admin/portal/menu/index', 1, 0, 'C', '0', '0', 'portal:menu:list', 'MenuOutlined', NOW(), NOW());
