-- ----------------------------
-- 爬虫模块菜单权限初始化脚本
-- ----------------------------

-- 1. 爬虫管理 (顶级目录)
DELETE FROM `sys_menu` WHERE `id` = 2000;
INSERT INTO `sys_menu` VALUES (2000, '爬虫管理', 0, 4, 'crawler', NULL, 1, 0, 'M', '0', '0', NULL, 'BugOutlined', now(), now());

-- 2. 任务管理 (二级菜单)
DELETE FROM `sys_menu` WHERE `id` BETWEEN 2001 AND 2010;
INSERT INTO `sys_menu` VALUES (2001, '任务管理', 2000, 1, 'task', 'crawler/task/index', 1, 0, 'C', '0', '0', 'crawler:task:list', 'ScheduleOutlined', now(), now());
-- 任务管理按钮权限
INSERT INTO `sys_menu` VALUES (2002, '任务查询', 2001, 1, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:query', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2003, '任务新增', 2001, 2, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:add', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2004, '任务修改', 2001, 3, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:edit', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2005, '任务删除', 2001, 4, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:remove', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2006, '任务启动', 2001, 5, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:start', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2007, '任务暂停', 2001, 6, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:pause', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2008, '任务恢复', 2001, 7, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:resume', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2009, '任务停止', 2001, 8, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:stop', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2010, '任务重跑', 2001, 9, '', NULL, 1, 0, 'F', '0', '0', 'crawler:task:rerun', '#', now(), now());

-- 3. 文章管理 (二级菜单)
DELETE FROM `sys_menu` WHERE `id` BETWEEN 2011 AND 2020;
INSERT INTO `sys_menu` VALUES (2011, '文章管理', 2000, 2, 'article', 'crawler/article/index', 1, 0, 'C', '0', '0', 'crawler:article:list', 'FileTextOutlined', now(), now());
-- 文章管理按钮权限
INSERT INTO `sys_menu` VALUES (2012, '文章查询', 2011, 1, '', NULL, 1, 0, 'F', '0', '0', 'crawler:article:query', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2013, '文章详情', 2011, 2, '', NULL, 1, 0, 'F', '0', '0', 'crawler:article:detail', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2014, '文章删除', 2011, 3, '', NULL, 1, 0, 'F', '0', '0', 'crawler:article:remove', '#', now(), now());

-- 4. 图片管理 (二级菜单)
DELETE FROM `sys_menu` WHERE `id` BETWEEN 2021 AND 2030;
INSERT INTO `sys_menu` VALUES (2021, '图片管理', 2000, 3, 'image', 'crawler/image/index', 1, 0, 'C', '0', '0', 'crawler:image:list', 'FileImageOutlined', now(), now());
-- 图片管理按钮权限
INSERT INTO `sys_menu` VALUES (2022, '图片查询', 2021, 1, '', NULL, 1, 0, 'F', '0', '0', 'crawler:image:query', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2023, '图片查看', 2021, 2, '', NULL, 1, 0, 'F', '0', '0', 'crawler:image:view', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2024, '图片删除', 2021, 3, '', NULL, 1, 0, 'F', '0', '0', 'crawler:image:remove', '#', now(), now());

-- 5. 代理配置 (二级菜单)
DELETE FROM `sys_menu` WHERE `id` BETWEEN 2031 AND 2040;
INSERT INTO `sys_menu` VALUES (2031, '代理配置', 2000, 4, 'proxy', 'crawler/proxy/index', 1, 0, 'C', '0', '0', 'crawler:proxy:list', 'GlobalOutlined', now(), now());
-- 代理配置按钮权限
INSERT INTO `sys_menu` VALUES (2032, '代理查询', 2031, 1, '', NULL, 1, 0, 'F', '0', '0', 'crawler:proxy:query', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2033, '代理新增', 2031, 2, '', NULL, 1, 0, 'F', '0', '0', 'crawler:proxy:add', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2034, '代理修改', 2031, 3, '', NULL, 1, 0, 'F', '0', '0', 'crawler:proxy:edit', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2035, '代理删除', 2031, 4, '', NULL, 1, 0, 'F', '0', '0', 'crawler:proxy:remove', '#', now(), now());

-- 6. 日志管理 (二级菜单)
DELETE FROM `sys_menu` WHERE `id` BETWEEN 2041 AND 2050;
INSERT INTO `sys_menu` VALUES (2041, '日志管理', 2000, 5, 'log', 'crawler/log/index', 1, 0, 'C', '0', '0', 'crawler:log:list', 'FileSearchOutlined', now(), now());
-- 日志管理按钮权限
INSERT INTO `sys_menu` VALUES (2042, '日志查询', 2041, 1, '', NULL, 1, 0, 'F', '0', '0', 'crawler:log:query', '#', now(), now());
INSERT INTO `sys_menu` VALUES (2043, '日志删除', 2041, 2, '', NULL, 1, 0, 'F', '0', '0', 'crawler:log:remove', '#', now(), now());

-- 7. 角色权限分配 (超级管理员 admin - ID 1)
-- 先删除该角色已有的爬虫相关权限关联（避免重复）
DELETE FROM `sys_role_menu` WHERE role_id = 1 AND menu_id BETWEEN 2000 AND 2050;
-- 插入新的权限关联
INSERT INTO `sys_role_menu` (role_id, menu_id) VALUES 
(1, 2000), 
(1, 2001), (1, 2002), (1, 2003), (1, 2004), (1, 2005), (1, 2006), (1, 2007), (1, 2008), (1, 2009), (1, 2010),
(1, 2011), (1, 2012), (1, 2013), (1, 2014),
(1, 2021), (1, 2022), (1, 2023), (1, 2024),
(1, 2031), (1, 2032), (1, 2033), (1, 2034), (1, 2035),
(1, 2041), (1, 2042), (1, 2043);

