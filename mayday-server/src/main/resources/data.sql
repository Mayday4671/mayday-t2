-- =============================================
-- MayDay Auth 测试数据初始化脚本
-- 执行顺序: 先执行 schema.sql，再执行本脚本
-- =============================================

-- 禁用外键检查
SET FOREIGN_KEY_CHECKS = 0;

-- 清空测试数据
TRUNCATE TABLE sys_role_menu;
TRUNCATE TABLE sys_role_dept;
TRUNCATE TABLE sys_user_role;
TRUNCATE TABLE sys_user_dept;
TRUNCATE TABLE sys_menu;
TRUNCATE TABLE sys_role;
TRUNCATE TABLE sys_dept;
TRUNCATE TABLE sys_user;

-- 启用外键检查
SET FOREIGN_KEY_CHECKS = 1;

-- =============================================
-- 1. 部门数据
-- =============================================
INSERT INTO sys_dept (id, parent_id, ancestors, dept_name, order_num) VALUES
(1, 0, '0',     '总公司',   0),
(2, 1, '0,1',   '技术部',   1),
(3, 1, '0,1',   '市场部',   2),
(4, 2, '0,1,2', '后端组',   1),
(5, 2, '0,1,2', '前端组',   2);

-- =============================================
-- 2. 用户数据 (密码: 123456)
-- BCrypt: $2a$10$/Fa124Wq2zcjZOcSkG6oNO21NrtzWMqPLGEbYhm/USNVUpGRbj7rW
-- =============================================
INSERT INTO sys_user (id, username, password, status) VALUES
(1, 'admin',    '$2a$10$/Fa124Wq2zcjZOcSkG6oNO21NrtzWMqPLGEbYhm/USNVUpGRbj7rW', '0'),
(2, 'zhangsan', '$2a$10$/Fa124Wq2zcjZOcSkG6oNO21NrtzWMqPLGEbYhm/USNVUpGRbj7rW', '0'),
(3, 'lisi',     '$2a$10$/Fa124Wq2zcjZOcSkG6oNO21NrtzWMqPLGEbYhm/USNVUpGRbj7rW', '0');

-- =============================================
-- 3. 用户-部门关联
-- =============================================
INSERT INTO sys_user_dept (user_id, dept_id, is_default) VALUES
(1, 1, 'Y'),  -- admin 属于总公司
(2, 2, 'Y'),  -- zhangsan 默认在技术部
(2, 3, 'N'),  -- zhangsan 兼职市场部
(3, 3, 'Y');  -- lisi 属于市场部

-- =============================================
-- 4. 角色数据
-- data_scope: 1=全部 2=自定义 3=本部门 4=本部门及以下 5=仅本人
-- =============================================
INSERT INTO sys_role (id, role_key, role_name, data_scope) VALUES
(1, 'admin',    '超级管理员', '1'),
(2, 'manager',  '部门经理',   '4'),
(3, 'employee', '普通员工',   '5');

-- =============================================
-- 5. 用户-角色关联
-- =============================================
INSERT INTO sys_user_role (user_id, role_id, dept_id) VALUES
(1, 1, 1),   -- admin 在总公司拥有管理员角色
(2, 2, 2),   -- zhangsan 在技术部是经理
(2, 3, 3),   -- zhangsan 在市场部是普通员工
(3, 3, 3);   -- lisi 在市场部是普通员工

-- =============================================
-- 6. 菜单/权限数据
-- menu_type: M=目录 C=菜单 F=按钮
-- =============================================
INSERT INTO sys_menu (id, menu_name, parent_id, order_num, path, component, menu_type, perms, icon, status) VALUES
-- 首页
(1, '首页', 0, 0, 'dashboard', 'dashboard/Index', 'C', NULL, 'DashboardOutlined', '0'),

-- 系统管理目录
(100, '系统管理', 0, 1, 'system', NULL, 'M', NULL, 'SettingOutlined', '0'),

-- 系统管理子菜单
(101, '用户管理', 100, 1, 'user', 'system/User', 'C', 'system:user:list', 'UserOutlined', '0'),
(102, '角色管理', 100, 2, 'role', 'system/Role', 'C', 'system:role:list', 'TeamOutlined', '0'),
(103, '部门管理', 100, 3, 'dept', 'system/Dept', 'C', 'system:dept:list', 'ApartmentOutlined', '0'),
(104, '菜单管理', 100, 4, 'menu', 'system/Menu', 'C', 'system:menu:list', 'MenuOutlined', '0'),

-- 系统监控目录
(200, '系统监控', 0, 2, 'monitor', NULL, 'M', NULL, 'FundViewOutlined', '0'),

-- 系统监控子菜单
(201, '在线用户', 200, 1, 'online', 'monitor/Online', 'C', 'monitor:online:list', 'UserSwitchOutlined', '0'),
(202, '数据监控', 200, 2, 'data', 'monitor/Data', 'C', 'monitor:data:list', 'AreaChartOutlined', '0'),

-- 用户管理按钮权限
(1001, '用户查询', 101, 1, '', NULL, 'F', 'system:user:query', '#', '0'),
(1002, '用户新增', 101, 2, '', NULL, 'F', 'system:user:add', '#', '0'),
(1003, '用户修改', 101, 3, '', NULL, 'F', 'system:user:edit', '#', '0'),
(1004, '用户删除', 101, 4, '', NULL, 'F', 'system:user:remove', '#', '0'),

-- 角色管理按钮权限
(1011, '角色查询', 102, 1, '', NULL, 'F', 'system:role:query', '#', '0'),
(1012, '角色新增', 102, 2, '', NULL, 'F', 'system:role:add', '#', '0'),
(1013, '角色修改', 102, 3, '', NULL, 'F', 'system:role:edit', '#', '0'),
(1014, '角色删除', 102, 4, '', NULL, 'F', 'system:role:remove', '#', '0'),

-- 部门管理按钮权限
(1021, '部门查询', 103, 1, '', NULL, 'F', 'system:dept:query', '#', '0'),
(1022, '部门新增', 103, 2, '', NULL, 'F', 'system:dept:add', '#', '0'),
(1023, '部门修改', 103, 3, '', NULL, 'F', 'system:dept:edit', '#', '0'),
(1024, '部门删除', 103, 4, '', NULL, 'F', 'system:dept:remove', '#', '0'),

-- 菜单管理按钮权限
(1031, '菜单查询', 104, 1, '', NULL, 'F', 'system:menu:query', '#', '0'),
(1032, '菜单新增', 104, 2, '', NULL, 'F', 'system:menu:add', '#', '0'),
(1033, '菜单修改', 104, 3, '', NULL, 'F', 'system:menu:edit', '#', '0'),
(1034, '菜单删除', 104, 4, '', NULL, 'F', 'system:menu:remove', '#', '0');

-- =============================================
-- 7. 角色-菜单关联
-- =============================================
-- 管理员拥有所有权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(1, 1),   -- 首页
(1, 100), (1, 101), (1, 102), (1, 103), (1, 104),  -- 系统管理
(1, 200), (1, 201), (1, 202),  -- 系统监控
(1, 1001), (1, 1002), (1, 1003), (1, 1004),  -- 用户按钮
(1, 1011), (1, 1012), (1, 1013), (1, 1014),  -- 角色按钮
(1, 1021), (1, 1022), (1, 1023), (1, 1024),  -- 部门按钮
(1, 1031), (1, 1032), (1, 1033), (1, 1034);  -- 菜单按钮

-- 部门经理有系统管理权限 (无菜单管理、系统监控)
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(2, 1),   -- 首页
(2, 100), (2, 101), (2, 102), (2, 103),  -- 系统管理 (无菜单管理)
(2, 1001), (2, 1002), (2, 1003),  -- 用户按钮 (无删除)
(2, 1011), (2, 1012), (2, 1013),  -- 角色按钮 (无删除)
(2, 1021), (2, 1022), (2, 1023);  -- 部门按钮 (无删除)

-- 普通员工只有首页和查询权限
INSERT INTO sys_role_menu (role_id, menu_id) VALUES
(3, 1),   -- 首页
(3, 100), (3, 101),  -- 系统管理 > 用户管理
(3, 1001);  -- 用户查询
-- 为角色 2 补充系统管理目录
INSERT IGNORE INTO sys_role_menu (role_id, menu_id) VALUES (2, 100);

-- 修复菜单 1035 的权限标识
UPDATE sys_menu SET perms = 'crawler:manage:list' WHERE id = 1035;


-- ----------------------------
-- Records of crawler_task
-- ----------------------------
INSERT INTO `crawler_task` VALUES (3, '青年美图', '', '[\"https://jrants.com/category/wanghong/twitter\"]', 'BOTH', 3, 'SITE', 2000, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 1, 'COMPLETED', 21, 21, 41, 0, '2025-12-14 19:30:47', '2026-01-10 22:20:42', '', '', '2025-12-14 19:30:34', '', '2026-01-10 22:16:54', '', 1, '', '.entry-content', '#right-sidebar');
INSERT INTO `crawler_task` VALUES (4, '草榴-张婉芳', '', '[\"https://www.t66y.com/thread0806.php?fid=16&search=377126\"]', 'BOTH', 3, 'SITE', 1000, 30000, 1, '', NULL, NULL, '', 1, NULL, 1, 0, 1, 'COMPLETED', 21, 10, 28, 0, '2025-12-14 19:32:12', '2025-12-20 20:34:54', '', '', '2025-12-14 19:31:55', '', '2025-12-18 23:25:43', '', 1, NULL, NULL, NULL);
INSERT INTO `crawler_task` VALUES (5, 'cl', '', '[\"https://www.t66y.com/thread0806.php?fid=16&search=&page=2\"]', 'BOTH', 3, 'SITE', 300, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 1, 'COMPLETED', 1, 10, 202, 0, '2025-12-15 00:27:31', '2025-12-15 23:42:07', '', '', '2025-12-15 00:27:28', '', '2025-12-16 00:09:35', '', 0, NULL, NULL, NULL);
INSERT INTO `crawler_task` VALUES (6, '偶像派', '', '[\"https://ouxpa.com/internet-celebrities-image-collection\"]', 'BOTH', 3, 'SITE', 1000, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 1, 'COMPLETED', 378, 230, 418, 0, '2025-12-15 21:28:26', '2025-12-24 23:31:42', '', '', '2025-12-15 21:28:23', '', '2025-12-24 23:40:45', '', 1, '.wp-block-columns', '', '');
INSERT INTO `crawler_task` VALUES (7, '虹图', '', '[\"https://www.hongimg.com/category/wanghong/%e7%a6%8f%e5%88%a9%e5%a7%ac/page/11\"]', 'BOTH', 3, 'SITE', 2000, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 50, 'COMPLETED', 85, 84, 164, 0, '2025-12-14 19:30:47', '2025-12-25 23:14:47', '', '', '2025-12-14 19:30:34', '', '2025-12-25 23:09:19', '', 1, '', '.entry-content', '#right-sidebar');

INSERT INTO `crawler_proxy` VALUES (1, 'USA', 'SOCKS', '127.0.0.1', 7897, '', '', 1, 0, '', '2025-12-14 22:31:20', '', '2025-12-14 22:31:20', '');






