<template>
  <a-menu
    v-model:selectedKeys="selectedKeys"
    v-model:openKeys="openKeys"
    theme="dark"
    mode="inline"
    @click="handleMenuClick"
  >
    <template v-for="menu in menus" :key="menu.path">
      <!-- 有子菜单的目录 -->
      <a-sub-menu v-if="menu.children && menu.children.length" :key="menu.path">
        <template #icon>
          <component :is="getIcon(menu.meta?.icon)" />
        </template>
        <template #title>{{ menu.meta?.title }}</template>
        <a-menu-item v-for="child in menu.children" :key="child.path">
          <template #icon>
            <component :is="getIcon(child.meta?.icon)" />
          </template>
          {{ child.meta?.title }}
        </a-menu-item>
      </a-sub-menu>
      <!-- 单个菜单项 -->
      <a-menu-item v-else :key="menu.path">
        <template #icon>
          <component :is="getIcon(menu.meta?.icon)" />
        </template>
        {{ menu.meta?.title }}
      </a-menu-item>
    </template>
  </a-menu>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import {
  DashboardOutlined,
  SettingOutlined,
  UserOutlined,
  TeamOutlined,
  ApartmentOutlined,
  MenuOutlined,
  FundViewOutlined,
  UserSwitchOutlined,
  AreaChartOutlined,
  AppstoreOutlined,
  BugOutlined,
  ScheduleOutlined,
  FileTextOutlined,
  PictureOutlined,
  CodeOutlined,
  GlobalOutlined,
  RobotOutlined,
  CommentOutlined,
} from "@ant-design/icons-vue";

interface MenuMeta {
  title: string;
  icon?: string;
}

interface MenuItem {
  name: string;
  path: string;
  component?: string;
  meta?: MenuMeta;
  children?: MenuItem[];
}

const props = defineProps<{
  menus: MenuItem[];
  collapsed: boolean;
}>();

const router = useRouter();
const route = useRoute();

const selectedKeys = ref<string[]>([]);
const openKeys = ref<string[]>([]);

// 图标映射
const iconMap: Record<string, any> = {
  DashboardOutlined,
  SettingOutlined,
  UserOutlined,
  TeamOutlined,
  ApartmentOutlined,
  MenuOutlined,
  FundViewOutlined,
  UserSwitchOutlined,
  AreaChartOutlined,
  AppstoreOutlined,
  BugOutlined,
  ScheduleOutlined,
  FileTextOutlined,
  PictureOutlined,
  CodeOutlined,
  GlobalOutlined,
  RobotOutlined,
  CommentOutlined,
};

const getIcon = (iconName?: string) => {
  if (!iconName || iconName === "#") return AppstoreOutlined;
  return iconMap[iconName] || AppstoreOutlined;
};

// 更新选中状态的函数 - 必须在 watch 之前定义
const updateSelectedKeys = (path: string) => {
  // 找到匹配的菜单项
  for (const menu of props.menus) {
    if (menu.path === path) {
      selectedKeys.value = [menu.path];
      return;
    }
    if (menu.children) {
      for (const child of menu.children) {
        const fullPath = `${menu.path}/${child.path}`;
        if (fullPath === path || path.startsWith(fullPath)) {
          selectedKeys.value = [child.path];
          openKeys.value = [menu.path];
          return;
        }
      }
    }
  }
};

// 菜单点击处理
const handleMenuClick = ({ key }: { key: string }) => {
  // 查找完整路径
  for (const menu of props.menus) {
    if (menu.path === key) {
      router.push(menu.path);
      return;
    }
    if (menu.children) {
      for (const child of menu.children) {
        if (child.path === key) {
          router.push(`${menu.path}/${child.path}`);
          return;
        }
      }
    }
  }
};

// 监听路由变化更新选中状态
watch(
  () => route.path,
  (path) => {
    updateSelectedKeys(path);
  },
);

// 监听菜单数据加载完成后更新选中状态
watch(
  () => props.menus,
  () => {
    updateSelectedKeys(route.path);
  },
  { deep: true }
);

// 组件挂载时初始化选中状态
onMounted(() => {
  updateSelectedKeys(route.path);
});
</script>
