-- ----------------------------
-- Table structure for sys_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_dept`;
CREATE TABLE `sys_dept` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '部门ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父部门ID',
  `ancestors` varchar(1000) DEFAULT '' COMMENT '祖级列表',
  `dept_name` varchar(100) DEFAULT '' COMMENT '部门名称',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(64) NOT NULL COMMENT '用户账号',
  `password` varchar(100) DEFAULT '' COMMENT '密码',
  `status` char(1) DEFAULT '0' COMMENT '帐号状态（0正常 1停用）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- ----------------------------
-- Table structure for sys_user_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_dept`;
CREATE TABLE `sys_user_dept` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  `is_default` char(1) DEFAULT 'N' COMMENT '是否默认部门(Y/N)',
  PRIMARY KEY (`user_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和部门关联表';

-- ----------------------------
-- Table structure for sys_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色ID',
  `role_key` varchar(100) NOT NULL COMMENT '角色权限字符串',
  `role_name` varchar(100) NOT NULL COMMENT '角色名称',
  `data_scope` char(1) DEFAULT '1' COMMENT '数据范围（1：全部数据权限 2：自定数据权限 3：本部门数据权限 4：本部门及以下数据权限 5：仅本人数据权限）',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色信息表';

-- ----------------------------
-- Table structure for sys_user_role
-- ----------------------------
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID (该角色在哪个部门生效)',
  PRIMARY KEY (`user_id`,`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户和角色关联表';

-- ----------------------------
-- Table structure for sys_role_dept
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_dept`;
CREATE TABLE `sys_role_dept` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `dept_id` bigint NOT NULL COMMENT '部门ID',
  PRIMARY KEY (`role_id`,`dept_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和部门关联表';

-- ----------------------------
-- Table structure for sys_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '菜单ID',
  `menu_name` varchar(50) NOT NULL COMMENT '菜单名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父菜单ID',
  `order_num` int DEFAULT '0' COMMENT '显示顺序',
  `path` varchar(200) DEFAULT '' COMMENT '路由地址',
  `component` varchar(255) DEFAULT NULL COMMENT '组件路径',
  `is_frame` int DEFAULT '1' COMMENT '是否为外链（0是 1否）',
  `is_cache` int DEFAULT '0' COMMENT '是否缓存（0缓存 1不缓存）',
  `menu_type` char(1) DEFAULT '' COMMENT '菜单类型（M目录 C菜单 F按钮）',
  `visible` char(1) DEFAULT '0' COMMENT '菜单状态（0显示 1隐藏）',
  `status` char(1) DEFAULT '0' COMMENT '菜单状态（0正常 1停用）',
  `perms` varchar(100) DEFAULT NULL COMMENT '权限标识',
  `icon` varchar(100) DEFAULT '#' COMMENT '菜单图标',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='菜单权限表';

-- ----------------------------
-- Table structure for sys_role_menu
-- ----------------------------
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `menu_id` bigint NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`role_id`,`menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色和菜单关联表';
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
