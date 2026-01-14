# MayDay Auth 项目进度文档

**更新时间**: 2026-01-12 17:55

---

## 1. 项目结构

```
mayday-t1-security/
├── pom.xml                      # 父 POM
├── mayday-auth/                 # 核心权限模块
│   └── src/main/java/com/mayday/auth/
│       ├── annotation/          # @DataScope
│       ├── aspect/              # DataScopeAspect
│       ├── common/              # R (统一响应)
│       ├── config/
│       │   ├── SecurityConfig   # Spring Security 6
│       │   ├── RedisConfig      # Redis 配置
│       │   ├── CorsConfig       # 跨域配置
│       │   ├── MyBatisFlexConfig
│       │   └── GlobalExceptionHandler
│       ├── controller/          # AuthController
│       ├── entity/              # 9 个实体类
│       ├── filter/              # JwtAuthenticationFilter
│       ├── mapper/              # 8 个 Mapper 接口
│       ├── model/               # LoginUser, DTOs, VOs
│       ├── service/
│       │   ├── SysLoginService  # 登录/选择部门/切换部门
│       │   ├── TokenService     # JWT + Redis
│       │   ├── PermissionService
│       │   └── UserDetailsServiceImpl
│       └── util/                # SecurityUtils
├── mayday-auth-demo/            # 演示模块
│   ├── src/main/java/.../DemoApplication.java
│   ├── src/main/java/.../controller/
│   │   ├── DemoController.java  # 权限演示
│   │   └── DebugController.java # 密码调试
│   └── src/main/resources/
│       ├── application.yml
│       ├── schema.sql
│       └── data.sql             # 测试数据
└── mayday-ui/                   # 前端 Vue3 项目
    └── src/
        ├── api/auth.ts
        ├── utils/request.ts
        ├── router/index.ts
        └── views/
            ├── Login.vue
            └── Home.vue
```

---

## 2. 当前配置

**application.yml (mayday-auth-demo)**
- 端口: `9002`
- 数据库: `mayday-t1-security`
- Redis: `localhost:6379`

---

## 3. 测试账号

| 用户名 | 密码 | 部门 |
|--------|------|------|
| admin | 123456 | 总公司(1) |
| zhangsan | 123456 | 技术部(2) + 市场部(3) 多部门 |
| lisi | 123456 | 市场部(3) |

---

## 4. 当前问题 ⚠️

**密码验证失败**: 使用 BCrypt 密码登录时报 "用户名或密码错误"

**调试步骤**:
1. 启动后端后访问: `http://localhost:9002/debug/encode?password=123456`
2. 获取新生成的 BCrypt 密码
3. 更新数据库:
   ```sql
   UPDATE sys_user SET password = '新密码' WHERE username = 'admin';
   ```
4. 重新测试登录

---

## 5. 启动命令

```bash
# 后端
cd mayday-t1-security/mayday-auth-demo
mvn spring-boot:run

# 前端
cd mayday-t1-security/mayday-ui
npm run dev
```

---

## 6. API 接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /login | 登录 (多部门返回 deptList) |
| POST | /selectDept | 选择部门 |
| POST | /switchDept | 切换部门 |
| POST | /logout | 登出 |
| GET | /getInfo | 获取用户信息 |
| GET | /debug/encode | 生成 BCrypt 密码 |
| GET | /debug/password | 验证密码匹配 |

---

## 7. 已完成功能

- [x] RBAC 权限模型 (用户/角色/菜单)
- [x] 多部门支持 (用户可属于多个部门)
- [x] 部门级角色 (同一用户在不同部门有不同角色)
- [x] JWT + Redis Token 管理
- [x] 数据权限 AOP (@DataScope)
- [x] 前端 Vue3 登录页面

---

## 8. 待解决

- [ ] 密码验证问题调试
- [ ] 完善前端 Home 页面功能
