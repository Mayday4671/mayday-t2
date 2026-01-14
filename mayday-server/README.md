# MayDay Auth Demo 使用指南

## 快速开始

### 1. 前置准备
```bash
# 确保 MySQL 和 Redis 已启动
# MySQL: localhost:3306, 数据库: mayday_auth
# Redis: localhost:6379
```

### 2. 初始化数据库
```sql
-- 先执行 schema.sql 创建表
source mayday-auth/src/main/resources/schema.sql

-- 再执行 data.sql 插入测试数据
source mayday-auth-demo/src/main/resources/data.sql
```

### 3. 启动应用
```bash
cd mayday-t1-security
mvn clean install -DskipTests
cd mayday-auth-demo
mvn spring-boot:run
```

---

## 测试账号

| 用户名 | 密码 | 特点 |
|--------|------|------|
| `admin` | `123456` | 超级管理员，全局权限 |
| `zhangsan` | `123456` | **多部门用户**：技术部(经理) + 市场部(员工) |
| `lisi` | `123456` | 市场部普通员工 |

---

## API 测试

### 1. 单部门用户登录 (lisi)
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"lisi","password":"123456"}'
```
响应:
```json
{
  "code": 200,
  "data": {
    "needSelectDept": false,
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "userId": 3,
    "currentDeptId": 102
  }
}
```

### 2. 多部门用户登录 (zhangsan) - 第一步
```bash
curl -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{"username":"zhangsan","password":"123456"}'
```
响应:
```json
{
  "code": 200,
  "data": {
    "needSelectDept": true,
    "tempToken": "eyJhbGciOiJIUzI1NiJ9...",
    "deptList": [
      {"deptId": 101, "deptName": "技术部", "isDefault": true},
      {"deptId": 102, "deptName": "市场部", "isDefault": false}
    ]
  }
}
```

### 3. 选择部门 - 第二步
```bash
curl -X POST http://localhost:8080/selectDept \
  -H "Content-Type: application/json" \
  -d '{"tempToken":"上一步返回的tempToken","deptId":101}'
```

### 4. 切换部门 (已登录)
```bash
curl -X POST http://localhost:8080/switchDept \
  -H "Authorization: Bearer {token}" \
  -H "Content-Type: application/json" \
  -d '{"deptId":102}'
```

### 5. 权限测试
```bash
# 查看当前用户
curl -H "Authorization: Bearer {token}" http://localhost:8080/demo/currentUser

# 测试权限 (需要 system:user:list)
curl -H "Authorization: Bearer {token}" http://localhost:8080/demo/users

# 测试数据权限 (观察生成的 SQL 条件)
curl -H "Authorization: Bearer {token}" http://localhost:8080/demo/dataScope
```

---

## 张三的角色变化演示

| 部门 | 角色 | 权限 |
|------|------|------|
| 技术部 (101) | manager | 用户增删改查 |
| 市场部 (102) | employee | 仅查询 |

**测试步骤:**
1. 张三选择技术部登录 → 可以删除用户
2. 切换到市场部 → 删除用户返回 403
