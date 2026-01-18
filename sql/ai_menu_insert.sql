-- 1. 插入顶级目录 "AI 智能助手"
-- parent_id=0, order_num=6 (排在其他后面), menu_type='M' (目录)
INSERT INTO `sys_menu` (
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, 
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, 
    `perms`, `icon`, `create_time`, `update_time`, `create_by`, `update_by`, `remark`
) VALUES (
    'AI 智能助手', 0, 6, 'ai', NULL, 
    1, 0, 'M', '0', '0', 
    NULL, 'RobotOutlined', NOW(), NOW(), 'admin', 'admin', 'AI模块功能入口'
);

-- 获取刚才插入的菜单ID (假设为 @parentId)
SET @parentId = LAST_INSERT_ID();

-- 2. 插入子菜单 "智能对话"
-- parent_id=@parentId, menu_type='C' (菜单)
INSERT INTO `sys_menu` (
    `menu_name`, `parent_id`, `order_num`, `path`, `component`, 
    `is_frame`, `is_cache`, `menu_type`, `visible`, `status`, 
    `perms`, `icon`, `create_time`, `update_time`, `create_by`, `update_by`, `remark`
) VALUES (
    '智能对话', @parentId, 1, 'chat', 'admin/ai/Chat', 
    1, 0, 'C', '0', '0', 
    'ai:chat:list', 'CommentOutlined', NOW(), NOW(), 'admin', 'admin', 'AI对话页面'
);

-- 3. (可选) 为管理员角色(role_id=1) 赋权
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, @parentId);
INSERT INTO `sys_role_menu` (`role_id`, `menu_id`) VALUES (1, LAST_INSERT_ID());
