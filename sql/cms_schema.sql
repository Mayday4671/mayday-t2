-- 1. 门户菜单表
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

-- 2. 文章分类表
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

-- 3. 修改文章表 (CrawlerArticleEntity)
-- 检查字段是否存在，不存在则添加 (MySQL 5.7+ 不支持 IF NOT EXISTS ADD COLUMN，故直接尝试添加，报错则说明已存在)
-- 建议人工执行或忽略重复报错
ALTER TABLE `crawler_article` ADD COLUMN `category_id` bigint(20) DEFAULT NULL COMMENT '分类ID' AFTER `task_id`;
ALTER TABLE `crawler_article` ADD COLUMN `view_count` bigint(20) DEFAULT '0' COMMENT '浏览量' AFTER `status`;
ALTER TABLE `crawler_article` ADD COLUMN `is_hot` int(1) DEFAULT '0' COMMENT '是否热门/置顶' AFTER `view_count`;
