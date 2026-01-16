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
      // You can add more public pages here e.g. /about, /product
    ],
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
        path: "system", // Keep relative path "system", so it becomes /admin/system/...
        // But wait, the original structure used specific children under /system.
        // Let's refactor to keep the sidebar menu working. 
        // Note: The SidebarMenu component likely uses route structure to generate menus.
        // If we nest everything under /admin, we need to ensure the menu logic still works.
        // For simplicity and robustness, let's keep the existing structure but serve it under the parent "/admin".
        redirect: "/admin/system/user", // Redirect /admin/system to /admin/system/user
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
