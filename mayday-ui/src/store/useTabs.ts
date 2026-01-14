import { ref, watch } from "vue";
import { useRouter } from "vue-router";

export interface TabItem {
  title: string;
  path: string;
  name: string;
  closable?: boolean;
}

const STORAGE_KEY = "MAYDAY_TABS";
const HOME_TAB: TabItem = {
  title: "首页",
  path: "/dashboard",
  name: "Dashboard",
  closable: false,
};

// 全局单例状态（如果不使用 Pinia）
const tabs = ref<TabItem[]>([HOME_TAB]);
const activeKey = ref<string>(HOME_TAB.path);

// 从缓存初始化
const savedTabs = localStorage.getItem(STORAGE_KEY);
if (savedTabs) {
  try {
    const parsed = JSON.parse(savedTabs);
    if (Array.isArray(parsed) && parsed.length > 0) {
      tabs.value = parsed;
    }
  } catch (e) {}
}

export function useTabs() {
  const router = useRouter();

  // 持久化存储
  watch(
    tabs,
    (newTabs: TabItem[]) => {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(newTabs));
    },
    { deep: true },
  );

  /**
   * 添加标签页
   */
  const addTab = (route: any) => {
    const path = route.path as string;
    const name = route.name as string;
    const meta = route.meta || {};

    if (!path || path === "/login") return;

    activeKey.value = path;

    // 如果已存在则不重复添加
    if (tabs.value.some((tab: TabItem) => tab.path === path)) return;

    tabs.value.push({
      title: (meta?.title as string) || "未知页面",
      path,
      name: name,
      closable: path !== "/dashboard",
    });
  };

  /**
   * 关闭指定标签页
   */
  const removeTab = (targetKey: string) => {
    if (targetKey === "/dashboard") return;

    let lastIndex = 0;
    tabs.value.forEach((tab: TabItem, i: number) => {
      if (tab.path === targetKey) {
        lastIndex = i - 1;
      }
    });

    const newTabs = tabs.value.filter((tab: TabItem) => tab.path !== targetKey);
    tabs.value = newTabs;

    if (newTabs.length && activeKey.value === targetKey) {
      const nextTab = newTabs[lastIndex >= 0 ? lastIndex : 0];
      if (nextTab) {
        activeKey.value = nextTab.path;
        router.push(activeKey.value);
      }
    }
  };

  /**
   * 关闭其他
   */
  const closeOthers = (currentPath: string) => {
    tabs.value = tabs.value.filter(
      (tab: TabItem) => !tab.closable || tab.path === currentPath,
    );
    activeKey.value = currentPath;
    router.push(currentPath);
  };

  /**
   * 关闭左侧
   */
  const closeLeft = (currentPath: string) => {
    const index = tabs.value.findIndex(
      (tab: TabItem) => tab.path === currentPath,
    );
    tabs.value = tabs.value.filter(
      (tab: TabItem, i: number) => i >= index || !tab.closable,
    );
    activeKey.value = currentPath;
    router.push(currentPath);
  };

  /**
   * 关闭右侧
   */
  const closeRight = (currentPath: string) => {
    const index = tabs.value.findIndex(
      (tab: TabItem) => tab.path === currentPath,
    );
    tabs.value = tabs.value.filter(
      (tab: TabItem, i: number) => i <= index || !tab.closable,
    );
    activeKey.value = currentPath;
    router.push(currentPath);
  };

  /**
   * 关闭所有 (保留首页)
   */
  const closeAll = () => {
    tabs.value = tabs.value.filter((tab: TabItem) => !tab.closable);
    activeKey.value = HOME_TAB.path;
    router.push(HOME_TAB.path);
  };

  /**
   * 重置标签页 (供退出登录使用)
   */
  const resetTabs = () => {
    tabs.value = [HOME_TAB];
    activeKey.value = HOME_TAB.path;
    router.push(HOME_TAB.path);
  };

  return {
    tabs,
    activeKey,
    addTab,
    removeTab,
    closeOthers,
    closeLeft,
    closeRight,
    closeAll,
    resetTabs,
  };
}
