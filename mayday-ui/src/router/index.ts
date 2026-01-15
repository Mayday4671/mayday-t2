import {
  createRouter,
  createWebHistory,
  type RouteRecordRaw,
} from "vue-router";
import Layout from "../layout/Layout.vue";

const routes: RouteRecordRaw[] = [
  {
    path: "/login",
    name: "Login",
    component: () => import("../views/Login.vue"),
  },
  {
    path: "/",
    component: Layout,
    redirect: "/dashboard",
    meta: { requiresAuth: true },
    children: [
      {
        path: "dashboard",
        name: "Dashboard",
        component: () => import("../views/dashboard/Index.vue"),
        meta: { title: "首页", icon: "DashboardOutlined" },
      },
      {
        path: "profile",
        name: "Profile",
        component: () => import("../views/profile/index.vue"),
        meta: { title: "个人中心", icon: "UserOutlined" },
      },
    ],
  },
  {
    path: "/system",
    component: Layout,
    name: "System",
    meta: { title: "系统管理", icon: "SettingOutlined", requiresAuth: true },
    children: [
      {
        path: "user",
        name: "User",
        component: () => import("../views/system/User.vue"),
        meta: { title: "用户管理", icon: "UserOutlined" },
      },
      {
        path: "role",
        name: "Role",
        component: () => import("../views/system/Role.vue"),
        meta: { title: "角色管理", icon: "TeamOutlined" },
      },
      {
        path: "dept",
        name: "Dept",
        component: () => import("../views/system/Dept.vue"),
        meta: { title: "部门管理", icon: "ApartmentOutlined" },
      },
      {
        path: "menu",
        name: "Menu",
        component: () => import("../views/system/Menu.vue"),
        meta: { title: "菜单管理", icon: "MenuOutlined" },
      },
    ],
  },
  // 爬虫管理 - 任务、代理、日志
  {
    path: "/crawler",
    component: Layout,
    name: "Crawler",
    meta: { title: "爬虫管理", icon: "BugOutlined", requiresAuth: true },
    children: [
      {
        path: "task",
        name: "CrawlerTask",
        component: () => import("../views/crawler/task/index.vue"),
        meta: { title: "任务管理", icon: "ScheduleOutlined" },
      },
      {
        path: "proxy",
        name: "CrawlerProxy",
        component: () => import("../views/crawler/proxy/index.vue"),
        meta: { title: "代理配置", icon: "GlobalOutlined" },
      },
      {
        path: "log",
        name: "CrawlerLog",
        component: () => import("../views/crawler/log/index.vue"),
        meta: { title: "日志管理", icon: "CodeOutlined" },
      },
    ],
  },
  // 文章管理 - 独立一级菜单，包含文章列表和图片列表
  {
    path: "/article",
    component: Layout,
    name: "Article",
    meta: { title: "文章管理", icon: "FileTextOutlined", requiresAuth: true },
    children: [
      {
        path: "list",
        name: "ArticleList",
        component: () => import("../views/crawler/article/index.vue"),
        meta: { title: "文章列表", icon: "UnorderedListOutlined" },
      },
      {
        path: "image",
        name: "ImageList",
        component: () => import("../views/crawler/image/index.vue"),
        meta: { title: "图片列表", icon: "PictureOutlined" },
      },
    ],
  },
  // 保留旧的 home 路由重定向
  {
    path: "/home",
    redirect: "/dashboard",
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
