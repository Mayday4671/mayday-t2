-- Sample Categories
INSERT INTO `cms_category` (`name`, `code`, `icon`, `sort`, `status`) VALUES 
('人工智能', 'ai', 'RobotOutlined', 1, 1),
('后端开发', 'backend', 'RocketOutlined', 2, 1),
('前端技术', 'frontend', 'AppstoreOutlined', 3, 1),
('移动开发', 'mobile', 'AndroidOutlined', 4, 1);

-- Sample Portal Menus
INSERT INTO `sys_portal_menu` (`name`, `path`, `icon`, `sort`, `target`, `status`) VALUES 
('首页', '/', 'HomeOutlined', 0, '_self', 1),
('GitHub', 'https://github.com/Mayday4671/mayday-t2', 'GithubOutlined', 2, '_blank', 1),
('关于我们', '/about', 'InfoCircleOutlined', 3, '_self', 1);
