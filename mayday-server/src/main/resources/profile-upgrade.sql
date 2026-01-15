-- ======================
-- 个人中心功能 - 用户表字段扩展
-- ======================

-- 添加用户个人信息字段
ALTER TABLE sys_user ADD COLUMN nickname VARCHAR(50) DEFAULT NULL COMMENT '用户昵称' AFTER password;
ALTER TABLE sys_user ADD COLUMN avatar VARCHAR(255) DEFAULT NULL COMMENT '用户头像URL' AFTER nickname;
ALTER TABLE sys_user ADD COLUMN email VARCHAR(100) DEFAULT NULL COMMENT '用户邮箱' AFTER avatar;
ALTER TABLE sys_user ADD COLUMN phone VARCHAR(20) DEFAULT NULL COMMENT '手机号码' AFTER email;

-- 更新现有用户的昵称为用户名（可选）
UPDATE sys_user SET nickname = username WHERE nickname IS NULL;
