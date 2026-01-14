/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80042 (8.0.42)
 Source Host           : localhost:3306
 Source Schema         : mayday-t1-security

 Target Server Type    : MySQL
 Target Server Version : 80042 (8.0.42)
 File Encoding         : 65001

 Date: 13/01/2026 22:15:31
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for crawler_article
-- ----------------------------
DROP TABLE IF EXISTS `crawler_article`;
CREATE TABLE `crawler_article`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '文章URL',
  `url_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL哈希值',
  `title` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文章标题',
  `content` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '文章正文',
  `summary` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文章摘要',
  `author` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '作者',
  `publish_time` datetime NULL DEFAULT NULL COMMENT '发布时间',
  `source_site` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '来源站点',
  `content_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '内容哈希值（用于增量判断）',
  `is_updated` tinyint NULL DEFAULT 0 COMMENT '是否已更新：0-否 1-是',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crawler_article_task`(`task_id` ASC) USING BTREE,
  INDEX `idx_crawler_article_hash`(`url_hash` ASC) USING BTREE,
  INDEX `idx_crawler_article_content_hash`(`content_hash` ASC) USING BTREE,
  INDEX `idx_crawler_article_pt`(`publish_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 934 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '文章表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crawler_article
-- ----------------------------

-- ----------------------------
-- Table structure for crawler_image
-- ----------------------------
DROP TABLE IF EXISTS `crawler_image`;
CREATE TABLE `crawler_image`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `article_id` bigint NULL DEFAULT NULL COMMENT '关联文章ID',
  `url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片URL',
  `url_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL哈希值',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '保存的文件名',
  `file_path` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件存储路径',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（字节）',
  `width` int NULL DEFAULT 0 COMMENT '图片宽度',
  `height` int NULL DEFAULT 0 COMMENT '图片高度',
  `format` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '图片格式（jpg/png/gif等）',
  `md5` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '文件MD5值（用于去重）',
  `download_status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '下载状态：PENDING-待下载 SUCCESS-成功 FAILED-失败',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '错误信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crawler_image_task`(`task_id` ASC) USING BTREE,
  INDEX `idx_crawler_image_article`(`article_id` ASC) USING BTREE,
  INDEX `idx_crawler_image_hash`(`url_hash` ASC) USING BTREE,
  INDEX `idx_crawler_image_md5`(`md5` ASC) USING BTREE,
  INDEX `idx_crawler_image_status`(`download_status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 14426 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '图片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crawler_image
-- ----------------------------

-- ----------------------------
-- Table structure for crawler_log
-- ----------------------------
DROP TABLE IF EXISTS `crawler_log`;
CREATE TABLE `crawler_log`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `log_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '日志类型：REQUEST-请求 PARSE-解析 DOWNLOAD-下载 ERROR-错误',
  `url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '相关URL',
  `message` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '日志消息',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'INFO' COMMENT '日志级别：DEBUG INFO WARN ERROR',
  `exception_info` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '异常信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crawler_log_task`(`task_id` ASC) USING BTREE,
  INDEX `idx_crawler_log_type`(`log_type` ASC) USING BTREE,
  INDEX `idx_crawler_log_level`(`level` ASC) USING BTREE,
  INDEX `idx_crawler_log_ct`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 65 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '爬虫日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crawler_log
-- ----------------------------

-- ----------------------------
-- Table structure for crawler_parse_rule
-- ----------------------------
DROP TABLE IF EXISTS `crawler_parse_rule`;
CREATE TABLE `crawler_parse_rule`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `rule_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则类型：TITLE-标题 CONTENT-正文 AUTHOR-作者 PUBLISH_TIME-发布时间',
  `rule_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '解析方法：CSS_SELECTOR XPATH',
  `rule_value` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '规则值',
  `priority` int NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高，用于多规则兜底）',
  `is_active` tinyint NULL DEFAULT 1 COMMENT '是否启用：0-否 1-是',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crawler_parse_rule_task`(`task_id` ASC) USING BTREE,
  INDEX `idx_crawler_parse_rule_type`(`rule_type` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '解析规则表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crawler_parse_rule
-- ----------------------------

-- ----------------------------
-- Table structure for crawler_proxy
-- ----------------------------
DROP TABLE IF EXISTS `crawler_proxy`;
CREATE TABLE `crawler_proxy`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `proxy_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代理名称',
  `proxy_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代理类型：HTTP / SOCKS',
  `host` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '代理主机',
  `port` int NOT NULL COMMENT '代理端口',
  `username` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '代理用户名（可选）',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '代理密码（可选）',
  `enabled` tinyint NULL DEFAULT 1 COMMENT '是否启用：0-否 1-是',
  `sort` int NULL DEFAULT 0 COMMENT '排序号（越小越靠前）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crawler_proxy_enabled`(`enabled` ASC) USING BTREE,
  INDEX `idx_crawler_proxy_sort`(`sort` ASC, `id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '爬虫全局代理配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crawler_proxy
-- ----------------------------
INSERT INTO `crawler_proxy` VALUES (1, 'USA', 'SOCKS', '127.0.0.1', 7897, '', '', 1, 0, '', '2025-12-14 22:31:20', '', '2025-12-14 22:31:20', '');

-- ----------------------------
-- Table structure for crawler_task
-- ----------------------------
DROP TABLE IF EXISTS `crawler_task`;
CREATE TABLE `crawler_task`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称',
  `task_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '任务描述',
  `start_urls` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '起始URL列表（JSON数组）',
  `crawl_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '爬取类型：ARTICLE-文章 IMAGE-图片 BOTH-文章+图片',
  `max_depth` int NULL DEFAULT 3 COMMENT '最大爬取深度',
  `scope_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'SITE' COMMENT '站点范围：SITE-仅站内 ALL-全站',
  `request_interval` int NULL DEFAULT 1000 COMMENT '请求间隔（毫秒）',
  `request_timeout` int NULL DEFAULT 30000 COMMENT '请求超时（毫秒）',
  `max_retries` int NULL DEFAULT 3 COMMENT '最大重试次数',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'User-Agent',
  `headers` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '自定义请求头（JSON）',
  `cookies` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'Cookie配置（JSON）',
  `referer` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'Referer',
  `use_proxy` tinyint NULL DEFAULT 0 COMMENT '是否使用代理：0-否 1-是',
  `proxy_list` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '代理列表（JSON数组）',
  `random_interval` tinyint NULL DEFAULT 0 COMMENT '是否随机化间隔：0-否 1-是',
  `rotate_user_agent` tinyint NULL DEFAULT 0 COMMENT '是否轮换User-Agent：0-否 1-是',
  `list_max_pages` int NULL DEFAULT 1 COMMENT '列表页最大翻页数（仅列表页场景，默认1）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'NOT_STARTED' COMMENT '任务状态：NOT_STARTED-未启动 RUNNING-运行中 PAUSED-已暂停 COMPLETED-已完成 ERROR-异常 STOPPED-已停止',
  `total_urls` int NULL DEFAULT 0 COMMENT '总URL数',
  `crawled_urls` int NULL DEFAULT 0 COMMENT '已爬取URL数',
  `success_count` int NULL DEFAULT 0 COMMENT '成功数',
  `error_count` int NULL DEFAULT 0 COMMENT '失败数',
  `start_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `error_msg` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '错误信息',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '',
  `download_images` tinyint NULL DEFAULT 0 COMMENT '是否下載圖片：0-否 1-是',
  `content_selector` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '正文容器选择器（CSS选择器，用于定位正文区域，如：#conttpc, .content, article等）',
  `image_selector` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片选择器（CSS选择器，用于定位正文中的图片，如：img, .post-content img等。如果为空，则从正文容器中提取所有img）',
  `exclude_selector` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '排除选择器（CSS选择器，用于排除不需要的区域，如：.related, .recommend, aside等）',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crawler_task_status`(`status` ASC) USING BTREE,
  INDEX `idx_crawler_task_ct`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 8 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '爬虫任务表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crawler_task
-- ----------------------------
INSERT INTO `crawler_task` VALUES (3, '青年美图', '', '[\"https://jrants.com/category/wanghong/twitter\"]', 'BOTH', 3, 'SITE', 2000, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 1, 'COMPLETED', 21, 21, 41, 0, '2025-12-14 19:30:47', '2026-01-10 22:20:42', '', '', '2025-12-14 19:30:34', '', '2026-01-10 22:16:54', '', 1, '', '.entry-content', '#right-sidebar');
INSERT INTO `crawler_task` VALUES (4, '草榴-张婉芳', '', '[\"https://www.t66y.com/thread0806.php?fid=16&search=377126\"]', 'BOTH', 3, 'SITE', 1000, 30000, 1, '', NULL, NULL, '', 1, NULL, 1, 0, 1, 'COMPLETED', 21, 10, 28, 0, '2025-12-14 19:32:12', '2025-12-20 20:34:54', '', '', '2025-12-14 19:31:55', '', '2025-12-18 23:25:43', '', 1, NULL, NULL, NULL);
INSERT INTO `crawler_task` VALUES (5, 'cl', '', '[\"https://www.t66y.com/thread0806.php?fid=16&search=&page=2\"]', 'BOTH', 3, 'SITE', 300, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 1, 'COMPLETED', 1, 10, 202, 0, '2025-12-15 00:27:31', '2025-12-15 23:42:07', '', '', '2025-12-15 00:27:28', '', '2025-12-16 00:09:35', '', 0, NULL, NULL, NULL);
INSERT INTO `crawler_task` VALUES (6, '偶像派', '', '[\"https://ouxpa.com/internet-celebrities-image-collection\"]', 'BOTH', 3, 'SITE', 1000, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 1, 'COMPLETED', 378, 230, 418, 0, '2025-12-15 21:28:26', '2025-12-24 23:31:42', '', '', '2025-12-15 21:28:23', '', '2025-12-24 23:40:45', '', 1, '.wp-block-columns', '', '');
INSERT INTO `crawler_task` VALUES (7, '虹图', '', '[\"https://www.hongimg.com/category/wanghong/%e7%a6%8f%e5%88%a9%e5%a7%ac/page/11\"]', 'BOTH', 3, 'SITE', 2000, 30000, 3, '', NULL, NULL, '', 1, NULL, 0, 0, 50, 'COMPLETED', 85, 84, 164, 0, '2025-12-14 19:30:47', '2025-12-25 23:14:47', '', '', '2025-12-14 19:30:34', '', '2025-12-25 23:09:19', '', 1, '', '.entry-content', '#right-sidebar');

-- ----------------------------
-- Table structure for crawler_url_queue
-- ----------------------------
DROP TABLE IF EXISTS `crawler_url_queue`;
CREATE TABLE `crawler_url_queue`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `task_id` bigint NOT NULL COMMENT '任务ID',
  `url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL地址',
  `url_hash` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT 'URL哈希值（用于去重）',
  `depth` int NULL DEFAULT 0 COMMENT 'URL深度',
  `priority` int NULL DEFAULT 0 COMMENT '优先级（数字越大优先级越高）',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'PENDING' COMMENT '状态：PENDING-待处理 PROCESSING-处理中 SUCCESS-成功 FAILED-失败',
  `retry_count` int NULL DEFAULT 0 COMMENT '重试次数',
  `error_msg` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '错误信息',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_crawler_url_queue_task`(`task_id` ASC) USING BTREE,
  INDEX `idx_crawler_url_queue_hash`(`url_hash` ASC) USING BTREE,
  INDEX `idx_crawler_url_queue_status`(`status` ASC) USING BTREE,
  INDEX `idx_crawler_url_queue_priority`(`priority` DESC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'URL队列表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of crawler_url_queue
-- ----------------------------

-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父部门ID',
  `ancestors` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '部门名称',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 13 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '部门表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_dept
-- ----------------------------
INSERT INTO `sys_dept` VALUES (1, 0, '0', '总公司', 0, NULL, NULL);
INSERT INTO `sys_dept` VALUES (2, 1, '0,1', '技术部0', 1, NULL, NULL);
INSERT INTO `sys_dept` VALUES (4, 2, '0,1,2', '后端组', 1, NULL, NULL);
INSERT INTO `sys_dept` VALUES (5, 2, '0,1,2', '前端组', 2, NULL, NULL);
INSERT INTO `sys_dept` VALUES (6, 0, '0', '测试部门', 0, NULL, NULL);
INSERT INTO `sys_dept` VALUES (7, 0, '0', '测试部门2', 0, NULL, NULL);
INSERT INTO `sys_dept` VALUES (8, 0, '0', '新部门', 0, NULL, NULL);
INSERT INTO `sys_dept` VALUES (9, 0, '0', 'NewDept', 0, NULL, NULL);
INSERT INTO `sys_dept` VALUES (10, 2, '0,1,2', 'SubDept', 0, NULL, NULL);
INSERT INTO `sys_dept` VALUES (12, 2, '0,1,2', '测试权限', 0, NULL, NULL);

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '菜单名称',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父菜单ID',
  `order_num` int NULL DEFAULT 0 COMMENT '显示顺序',
  `path` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '组件路径',
  `is_frame` int NULL DEFAULT 1 COMMENT '是否为外链（0是 1否）',
  `is_cache` int NULL DEFAULT 0 COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '#' COMMENT '菜单图标',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1036 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '菜单权限表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_menu
-- ----------------------------
INSERT INTO `sys_menu` VALUES (1, '首页', 0, 0, 'dashboard', 'dashboard/Index', 1, 0, 'C', '0', '0', NULL, 'DashboardOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (100, '系统管理', 0, 1, 'system', NULL, 1, 0, 'M', '0', '0', NULL, 'SettingOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (101, '用户管理', 100, 1, 'user', 'system/User', 1, 0, 'C', '0', '0', 'system:user:list', 'UserOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (102, '角色管理', 100, 2, 'role', 'system/Role', 1, 0, 'C', '0', '0', 'system:role:list', 'TeamOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (103, '部门管理', 100, 3, 'dept', 'system/Dept', 1, 0, 'C', '0', '0', 'system:dept:list', 'ApartmentOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (104, '菜单管理', 100, 4, 'menu', 'system/Menu', 1, 0, 'C', '0', '0', 'system:menu:list', 'MenuOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (200, '系统监控', 0, 2, 'monitor', NULL, 1, 0, 'M', '0', '0', NULL, 'FundViewOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (201, '在线用户', 200, 1, 'online', 'monitor/Online', 1, 0, 'C', '0', '0', 'monitor:online:list', 'UserSwitchOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (202, '数据监控', 200, 2, 'data', 'monitor/Data', 1, 0, 'C', '0', '0', 'monitor:data:list', 'AreaChartOutlined', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1001, '用户查询', 101, 1, '', NULL, 1, 0, 'F', '0', '0', 'system:user:query', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1002, '用户新增', 101, 2, '', NULL, 1, 0, 'F', '0', '0', 'system:user:add', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1003, '用户修改', 101, 3, '', NULL, 1, 0, 'F', '0', '0', 'system:user:edit', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1004, '用户删除', 101, 4, '', NULL, 1, 0, 'F', '0', '0', 'system:user:remove', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1011, '角色查询', 102, 1, '', NULL, 1, 0, 'F', '0', '0', 'system:role:query', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1012, '角色新增', 102, 2, '', NULL, 1, 0, 'F', '0', '0', 'system:role:add', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1013, '角色修改', 102, 3, '', NULL, 1, 0, 'F', '0', '0', 'system:role:edit', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1014, '角色删除', 102, 4, '', NULL, 1, 0, 'F', '0', '0', 'system:role:remove', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1021, '部门查询', 103, 1, '', NULL, 1, 0, 'F', '0', '0', 'system:dept:query', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1022, '部门新增', 103, 2, '', NULL, 1, 0, 'F', '0', '0', 'system:dept:add', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1023, '部门修改', 103, 3, '', NULL, 1, 0, 'F', '0', '0', 'system:dept:edit', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1024, '部门删除', 103, 4, '', NULL, 1, 0, 'F', '0', '0', 'system:dept:remove', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1031, '菜单查询', 104, 1, '', NULL, 1, 0, 'F', '0', '0', 'system:menu:query', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1032, '菜单新增', 104, 2, '', NULL, 1, 0, 'F', '0', '0', 'system:menu:add', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1033, '菜单修改', 104, 3, '', NULL, 1, 0, 'F', '0', '0', 'system:menu:edit', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1034, '菜单删除', 104, 4, '', NULL, 1, 0, 'F', '0', '0', 'system:menu:remove', '#', NULL, NULL);
INSERT INTO `sys_menu` VALUES (1035, '爬虫管理', 0, 3, 'A	', 'A', NULL, NULL, 'C', NULL, '0', 'crawler:manage:list', NULL, NULL, NULL);

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色权限字符串',
  `role_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色名称',
  `data_scope` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role
-- ----------------------------
INSERT INTO `sys_role` VALUES (1, 'admin', '超级管理员', '1', NULL, NULL);
INSERT INTO `sys_role` VALUES (2, 'manager', '部门经理', '4', NULL, NULL);
INSERT INTO `sys_role` VALUES (3, 'employee', '普通员工', '5', NULL, NULL);

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_dept
-- ----------------------------

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu`  (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`, `menu_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '角色和菜单关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_role_menu
-- ----------------------------
INSERT INTO `sys_role_menu` VALUES (1, 1);
INSERT INTO `sys_role_menu` VALUES (1, 100);
INSERT INTO `sys_role_menu` VALUES (1, 101);
INSERT INTO `sys_role_menu` VALUES (1, 102);
INSERT INTO `sys_role_menu` VALUES (1, 103);
INSERT INTO `sys_role_menu` VALUES (1, 104);
INSERT INTO `sys_role_menu` VALUES (1, 200);
INSERT INTO `sys_role_menu` VALUES (1, 201);
INSERT INTO `sys_role_menu` VALUES (1, 202);
INSERT INTO `sys_role_menu` VALUES (1, 1001);
INSERT INTO `sys_role_menu` VALUES (1, 1002);
INSERT INTO `sys_role_menu` VALUES (1, 1003);
INSERT INTO `sys_role_menu` VALUES (1, 1004);
INSERT INTO `sys_role_menu` VALUES (1, 1011);
INSERT INTO `sys_role_menu` VALUES (1, 1012);
INSERT INTO `sys_role_menu` VALUES (1, 1013);
INSERT INTO `sys_role_menu` VALUES (1, 1014);
INSERT INTO `sys_role_menu` VALUES (1, 1021);
INSERT INTO `sys_role_menu` VALUES (1, 1022);
INSERT INTO `sys_role_menu` VALUES (1, 1023);
INSERT INTO `sys_role_menu` VALUES (1, 1024);
INSERT INTO `sys_role_menu` VALUES (1, 1031);
INSERT INTO `sys_role_menu` VALUES (1, 1032);
INSERT INTO `sys_role_menu` VALUES (1, 1033);
INSERT INTO `sys_role_menu` VALUES (1, 1034);
INSERT INTO `sys_role_menu` VALUES (2, 1);
INSERT INTO `sys_role_menu` VALUES (2, 100);
INSERT INTO `sys_role_menu` VALUES (2, 101);
INSERT INTO `sys_role_menu` VALUES (2, 103);
INSERT INTO `sys_role_menu` VALUES (2, 104);
INSERT INTO `sys_role_menu` VALUES (2, 200);
INSERT INTO `sys_role_menu` VALUES (2, 201);
INSERT INTO `sys_role_menu` VALUES (2, 202);
INSERT INTO `sys_role_menu` VALUES (2, 1001);
INSERT INTO `sys_role_menu` VALUES (2, 1002);
INSERT INTO `sys_role_menu` VALUES (2, 1003);
INSERT INTO `sys_role_menu` VALUES (2, 1004);
INSERT INTO `sys_role_menu` VALUES (2, 1021);
INSERT INTO `sys_role_menu` VALUES (2, 1022);
INSERT INTO `sys_role_menu` VALUES (2, 1023);
INSERT INTO `sys_role_menu` VALUES (2, 1024);
INSERT INTO `sys_role_menu` VALUES (2, 1031);
INSERT INTO `sys_role_menu` VALUES (2, 1032);
INSERT INTO `sys_role_menu` VALUES (2, 1033);
INSERT INTO `sys_role_menu` VALUES (2, 1034);
INSERT INTO `sys_role_menu` VALUES (2, 1035);
INSERT INTO `sys_role_menu` VALUES (3, 1);
INSERT INTO `sys_role_menu` VALUES (3, 100);
INSERT INTO `sys_role_menu` VALUES (3, 101);
INSERT INTO `sys_role_menu` VALUES (3, 1001);

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户账号',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '密码',
  `status` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 5 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$/Fa124Wq2zcjZOcSkG6oNO21NrtzWMqPLGEbYhm/USNVUpGRbj7rW', '0', NULL, NULL);
INSERT INTO `sys_user` VALUES (2, 'zhangsan', '$2a$10$/Fa124Wq2zcjZOcSkG6oNO21NrtzWMqPLGEbYhm/USNVUpGRbj7rW', '0', NULL, NULL);
INSERT INTO `sys_user` VALUES (3, 'lisi', '$2a$10$/Fa124Wq2zcjZOcSkG6oNO21NrtzWMqPLGEbYhm/USNVUpGRbj7rW', '0', NULL, NULL);
INSERT INTO `sys_user` VALUES (4, 'mayday', '$2a$10$p4nFSk6R4Hi5QZJiRXs9t.YHbGlI4FSLEbYOUNBwbmskhYdwrNTSa', '0', NULL, NULL);

-- ----------------------------
-- Table structure for sys_user_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_dept`;
CREATE TABLE `sys_user_dept`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `is_default` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '是否默认部门(Y/N)',
  PRIMARY KEY (`user_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和部门关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_dept
-- ----------------------------
INSERT INTO `sys_user_dept` VALUES (1, 1, 'Y');
INSERT INTO `sys_user_dept` VALUES (2, 2, 'Y');
INSERT INTO `sys_user_dept` VALUES (2, 3, 'N');
INSERT INTO `sys_user_dept` VALUES (3, 3, 'Y');
INSERT INTO `sys_user_dept` VALUES (4, 2, 'Y');
INSERT INTO `sys_user_dept` VALUES (4, 3, 'N');

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role`  (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID (该角色在哪个部门生效)',
  PRIMARY KEY (`user_id`, `role_id`, `dept_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户和角色关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user_role
-- ----------------------------
INSERT INTO `sys_user_role` VALUES (1, 1, 1);
INSERT INTO `sys_user_role` VALUES (2, 2, 2);
INSERT INTO `sys_user_role` VALUES (2, 3, 3);
INSERT INTO `sys_user_role` VALUES (3, 3, 3);
INSERT INTO `sys_user_role` VALUES (4, 2, 2);
INSERT INTO `sys_user_role` VALUES (4, 2, 3);

SET FOREIGN_KEY_CHECKS = 1;
