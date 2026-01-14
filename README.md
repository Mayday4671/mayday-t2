# MayDay Auth 项目 - 快速启动指南

## 环境要求
- Java 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+

## 一、数据库初始化

```sql
CREATE DATABASE `mayday-t1-security` DEFAULT CHARACTER SET utf8mb4;
USE `mayday-t1-security`;

-- 执行建表和测试数据
source E:/My_Project/mayday-common-security/mayday-t1-security/mayday-auth/src/main/resources/schema.sql;
source E:/My_Project/mayday-common-security/mayday-t1-security/mayday-auth-demo/src/main/resources/data.sql;
```

## 二、启动后端

```bash
cd E:/My_Project/mayday-common-security/mayday-t1-security/mayday-auth-demo
mvn spring-boot:run
```
访问: http://localhost:9002

## 三、启动前端

```bash
cd E:/My_Project/mayday-common-security/mayday-t1-security/mayday-ui
npm install
npm run dev
```
访问: http://localhost:5173

## 四、当前调试问题

**密码验证失败解决方案:**

1. 访问 http://localhost:9002/debug/encode?password=123456
2. 复制返回的 BCrypt 密码
3. 更新数据库:
```sql
UPDATE sys_user SET password = '生成的密码';
```
4. 重新登录测试

## 五、测试账号

| 用户 | 密码 | 说明 |
|------|------|------|
| admin | 123456 | 管理员 |
| zhangsan | 123456 | 多部门用户 |
| lisi | 123456 | 普通用户 |
