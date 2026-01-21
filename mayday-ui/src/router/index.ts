import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import Layout from "../layout/admin/Layout.vue";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/admin/Login.vue"),
  },
  // --- PUBLIC ROUTES (Frontend) ---
  {
    path: "/",
    component: () => import("../layout/frontend/Layout.vue"),
    children: [
      {
        path: "",
        name: "Home",
        component: () => import("../views/frontend/home/Index.vue"),
        meta: { title: "Home" }, // Public, no auth required
      },
    ],
  },

  // Independent Article Detail Route (No Layout Wrapper)
  {
    path: "/article/:id",
    name: "ArticleDetail",
    component: () => import("../views/frontend/article/Detail.vue"),
    meta: { title: "文章详情" },
  },

  // --- ADMIN ROUTES (Backend) ---
  {
    path: "/admin", // Changed from "/" to "/admin"
    component: Layout,
    // When visiting /admin, redirect to /admin/dashboard
    redirect: "/admin/dashboard",
    meta: { requiresAuth: true },
    children: [
      // 1. Dashboard
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("../views/admin/dashboard/Index.vue"),
        meta: { title: "首页", icon: "DashboardOutlined" },
      },
      // 2. Profile
      {
        path: "profile",
        name: "Profile",
        component: () => import("../views/admin/profile/index.vue"),
        meta: { title: "个人中心", icon: "UserOutlined" },
      },
      // 3. System Management
      {
        path: "system",
        redirect: "/admin/system/user",
        meta: { title: "系统管理", icon: "SettingOutlined" },
        children: [
          {
            path: "user",
            name: "User",
            component: () => import("../views/admin/system/User.vue"),
            meta: { title: "用户管理", icon: "UserOutlined" },
          },
          {
            path: "role",
            name: "Role",
            component: () => import("../views/admin/system/Role.vue"),
            meta: { title: "角色管理", icon: "TeamOutlined" },
          },
          {
            path: "dept",
            name: "Dept",
            component: () => import("../views/admin/system/Dept.vue"),
            meta: { title: "部门管理", icon: "ApartmentOutlined" },
          },
          {
            path: "menu",
            name: "Menu",
            component: () => import("../views/admin/system/Menu.vue"),
            meta: { title: "菜单管理", icon: "MenuOutlined" },
          },
        ],
      },
      // 4. Crawler Management
      {
        path: "crawler",
        redirect: "/admin/crawler/task",
        meta: { title: "爬虫管理", icon: "BugOutlined" },
        children: [
          {
            path: "task",
            name: "CrawlerTask",
            component: () => import("../views/admin/crawler/task/index.vue"),
            meta: { title: "任务管理", icon: "ScheduleOutlined" },
          },
          {
            path: "proxy",
            name: "CrawlerProxy",
            component: () => import("../views/admin/crawler/proxy/index.vue"),
            meta: { title: "代理配置", icon: "GlobalOutlined" },
          },
          {
            path: "log",
            name: "CrawlerLog",
            component: () => import("../views/admin/crawler/log/index.vue"),
            meta: { title: "日志管理", icon: "CodeOutlined" },
          },
        ],
      },
      // 5. Article Management
      {
        path: "article",
        redirect: "/admin/article/list",
        meta: { title: "文章管理", icon: "FileTextOutlined" },
        children: [
          {
            path: "list",
            name: "ArticleList",
            component: () => import("../views/admin/crawler/article/index.vue"),
            meta: { title: "文章列表", icon: "UnorderedListOutlined" },
          },
          {
            path: "image",
            name: "ImageList",
            component: () => import("../views/admin/crawler/image/index.vue"),
            meta: { title: "图片列表", icon: "PictureOutlined" },
          },
        ],
      },
      // 6. AI Module
      {
        path: "ai",
        redirect: "/admin/ai/chat",
        meta: { title: "AI 智能助手", icon: "RobotOutlined" },
        children: [
          {
            path: "chat",
            name: "AiChat",
            component: () => import("../views/admin/ai/Chat.vue"),
            meta: { title: "智能对话", icon: "CommentOutlined" }
          }
        ]
      },
      // 7. AI Configuration Management
      {
        path: "ai-config-manage",
        meta: { title: "AI 配置管理", icon: "RobotOutlined" },
        children: [
          {
            path: "ai-key",
            name: "AiKey",
            component: () => import("../views/admin/ai/AiKey.vue"),
            meta: { title: "密钥管理", icon: "KeyOutlined" }
          },
          {
            path: "ai-config",
            name: "AiConfig",
            component: () => import("../views/admin/ai/AiConfig.vue"),
            meta: { title: "路由配置", icon: "DeploymentUnitOutlined" }
          },
          {
            path: "ai-log",
            name: "AiCallLog",
            component: () => import("../views/admin/ai/AiCallLog.vue"),
            meta: { title: "调用日志", icon: "FileTextOutlined" }
          }
        ]
      }
    ],
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 路由守卫
router.beforeEach((to, _from, next) => {
  const token = localStorage.getItem("token");
  if (to.meta.requiresAuth && !token) {
    next("/login");
  } else {
    next();
  }
});

export default router;
