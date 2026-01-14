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
  {
    path: "/crawler",
    component: Layout,
    name: "Crawler",
    meta: { title: "网络爬虫", icon: "BugOutlined", requiresAuth: true },
    children: [
      {
        path: "task",
        name: "CrawlerTask",
        component: () => import("../views/crawler/task/index.vue"),
        meta: { title: "任务管理", icon: "ScheduleOutlined" },
      },
      {
        path: "article",
        name: "CrawlerArticle",
        component: () => import("../views/crawler/article/index.vue"),
        meta: { title: "文章管理", icon: "FileTextOutlined" },
      },
      {
        path: "image",
        name: "CrawlerImage",
        component: () => import("../views/crawler/image/index.vue"),
        meta: { title: "图片管理", icon: "PictureOutlined" },
      },
      {
        path: "log",
        name: "CrawlerLog",
        component: () => import("../views/crawler/log/index.vue"),
        meta: { title: "爬虫日志", icon: "CodeOutlined" },
      },
      {
        path: "proxy",
        name: "CrawlerProxy",
        component: () => import("../views/crawler/proxy/index.vue"),
        meta: { title: "代理设置", icon: "GlobalOutlined" },
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
