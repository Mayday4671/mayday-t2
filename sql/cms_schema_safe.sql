-- 1. Create sys_portal_menu
CREATE TABLE IF NOT EXISTS `sys_portal_menu` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '菜单名称',
  `path` varchar(255) DEFAULT NULL COMMENT '跳转链接',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `target` varchar(20) DEFAULT '_self' COMMENT '跳转方式',
  `status` int(1) DEFAULT '1' COMMENT '状态 1:启用 0:停用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='门户菜单表';

-- 2. Create cms_category
CREATE TABLE IF NOT EXISTS `cms_category` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `code` varchar(50) NOT NULL COMMENT '分类编码',
  `icon` varchar(100) DEFAULT NULL COMMENT '图标',
  `sort` int(11) DEFAULT '0' COMMENT '排序',
  `status` int(1) DEFAULT '1' COMMENT '状态 1:启用 0:停用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文章分类表';

-- 3. Alter crawler_article (Separate statements to avoid failure if one exists)
-- Using store procedure or just ignoring error is hard in raw SQL script without delimiter.
-- I'll use simple ALTER IGNORE syntax concept: Just try to add them. If exist, it errors but that's fine for now if I run line by line.
-- Actually, better to check INFORMATION_SCHEMA? No, too complex.
-- I will run these 3 ALTERs separately via command line loop.

