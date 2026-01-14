<template>
  <div class="home-container">
    <a-layout style="min-height: 100vh">
      <a-layout-header class="header">
        <div class="logo">🔐 MayDay Auth Demo</div>
        <div class="user-info">
          <a-space>
            <span>👤 {{ userInfo?.username }}</span>
            <!-- 部门切换下拉框 -->
            <a-select
              v-if="deptList.length > 1"
              v-model:value="currentDeptId"
              @change="handleSwitchDept"
              style="width: 140px"
              :loading="switching"
            >
              <a-select-option
                v-for="dept in deptList"
                :key="dept.deptId"
                :value="dept.deptId"
              >
                {{ dept.deptName }}
                <span v-if="dept.isDefault" style="color: #1890ff">(默认)</span>
              </a-select-option>
            </a-select>
            <a-tag v-else color="blue">部门: {{ currentDeptName }}</a-tag>
            <a-button type="link" @click="handleLogout">退出</a-button>
          </a-space>
        </div>
      </a-layout-header>

      <a-layout-content class="content">
        <!-- 当前用户信息 -->
        <a-row :gutter="16">
          <a-col :span="12">
            <a-card title="📋 当前用户信息">
              <a-descriptions :column="1" bordered size="small">
                <a-descriptions-item label="用户ID">{{
                  userInfo?.userId
                }}</a-descriptions-item>
                <a-descriptions-item label="用户名">{{
                  userInfo?.username
                }}</a-descriptions-item>
                <a-descriptions-item label="当前部门">
                  <a-tag color="blue">{{ currentDeptName }}</a-tag>
                  <span
                    v-if="deptList.length > 1"
                    style="color: #999; font-size: 12px"
                  >
                    (共 {{ deptList.length }} 个部门)
                  </span>
                </a-descriptions-item>
                <a-descriptions-item label="数据权限范围">
                  <a-tag :color="getDataScopeColor(dataScopeInfo)">{{
                    dataScopeInfo
                  }}</a-tag>
                </a-descriptions-item>
                <a-descriptions-item label="权限数量">{{
                  userInfo?.permissions?.length || 0
                }}</a-descriptions-item>
              </a-descriptions>
            </a-card>
          </a-col>

          <a-col :span="12">
            <a-card title="🔑 权限列表">
              <div v-if="userInfo?.permissions?.length">
                <a-tag
                  v-for="perm in userInfo.permissions"
                  :key="perm"
                  color="blue"
                  style="margin: 4px"
                >
                  {{ perm }}
                </a-tag>
              </div>
              <a-empty v-else description="暂无权限" />
            </a-card>
          </a-col>
        </a-row>

        <!-- 数据权限表格 -->
        <a-card title="📊 用户列表（数据权限演示）" style="margin-top: 16px">
          <template #extra>
            <a-button type="primary" @click="loadUserList" :loading="loading">
              刷新数据
            </a-button>
          </template>

          <a-alert style="margin-bottom: 16px" type="info" show-icon>
            <template #message>
              <span>
                当前数据权限: <strong>{{ dataScopeInfo }}</strong> | 可见用户数:
                <strong>{{ userList.length }}</strong> 人
              </span>
            </template>
            <template #description>
              <div style="font-size: 12px; color: #666">
                <strong>SQL 条件: </strong>
                <code>{{
                  dataScopeSql || "无条件（管理员拥有全部权限）"
                }}</code>
              </div>
            </template>
          </a-alert>

          <a-table
            :columns="columns"
            :dataSource="userList"
            :loading="loading"
            rowKey="userId"
            :pagination="{ pageSize: 10 }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'status'">
                <a-tag :color="record.status === '正常' ? 'green' : 'red'">
                  {{ record.status }}
                </a-tag>
              </template>
              <template v-if="column.key === 'deptNames'">
                <a-tag
                  v-for="dept in record.deptNames"
                  :key="dept"
                  color="purple"
                  style="margin: 2px"
                >
                  {{ dept }}
                </a-tag>
              </template>
            </template>
          </a-table>
        </a-card>

        <!-- 测试说明 -->
        <a-card title="📝 测试说明" style="margin-top: 16px">
          <a-descriptions :column="1" bordered size="small">
            <a-descriptions-item label="admin">
              管理员 (data_scope=1)，可以看到<strong>所有用户</strong>
            </a-descriptions-item>
            <a-descriptions-item label="zhangsan">
              <strong>多部门用户</strong
              >：可在右上角切换部门，切换后权限和数据范围会变化<br />
              技术部: 经理角色 (data_scope=4) | 市场部: 员工角色 (data_scope=5)
            </a-descriptions-item>
            <a-descriptions-item label="lisi">
              普通员工 (data_scope=5)，只能看到<strong>自己</strong>
            </a-descriptions-item>
          </a-descriptions>
        </a-card>
      </a-layout-content>
    </a-layout>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from "vue";
import { useRouter } from "vue-router";
import {
  getInfo,
  logout,
  switchDept,
  type LoginResult,
  type DeptOption,
} from "../api/auth";
import { message } from "ant-design-vue";
import request from "../utils/request";

const router = useRouter();
const userInfo = ref<LoginResult>();
const loading = ref(false);
const switching = ref(false);
const userList = ref<any[]>([]);
const dataScopeSql = ref("");
const dataScopeInfo = ref("未知");
const deptList = ref<DeptOption[]>([]);
const currentDeptId = ref<number>();

// 当前部门名称
const currentDeptName = computed(() => {
  if (!currentDeptId.value) return "无";
  const dept = deptList.value.find((d) => d.deptId === currentDeptId.value);
  return dept?.deptName || `部门 ${currentDeptId.value}`;
});

// 表格列定义
const columns = [
  { title: "用户ID", dataIndex: "userId", key: "userId", width: 80 },
  { title: "用户名", dataIndex: "username", key: "username", width: 120 },
  { title: "状态", dataIndex: "status", key: "status", width: 80 },
  { title: "所属部门", dataIndex: "deptNames", key: "deptNames" },
];

onMounted(async () => {
  // 从 localStorage 读取部门列表
  const savedDeptList = localStorage.getItem("deptList");
  if (savedDeptList) {
    try {
      deptList.value = JSON.parse(savedDeptList);
    } catch (e) {}
  }

  await loadUserInfo();
  await loadUserList();
});

const loadUserInfo = async () => {
  try {
    const res = await getInfo();
    userInfo.value = res;
    currentDeptId.value = res.currentDeptId;
  } catch (e) {
    router.push("/login");
  }
};

const loadUserList = async () => {
  loading.value = true;
  try {
    const res = await request.get("/demo/userList");
    userList.value = res.userList || [];
    dataScopeSql.value = res.dataScopeSql || "";
    dataScopeInfo.value = res.dataScope || "未知";
  } catch (e: any) {
    console.error("加载用户列表失败:", e);
  } finally {
    loading.value = false;
  }
};

// 切换部门
const handleSwitchDept = async (deptId: number) => {
  switching.value = true;
  try {
    const res = await switchDept(deptId);
    // 更新 token
    localStorage.setItem("token", res.token!);
    // 重新加载用户信息和数据
    await loadUserInfo();
    await loadUserList();
    message.success(`已切换到 ${currentDeptName.value}`);
  } catch (e: any) {
    message.error(e.message || "切换部门失败");
    // 恢复原来的部门
    currentDeptId.value = userInfo.value?.currentDeptId;
  } finally {
    switching.value = false;
  }
};

const getDataScopeColor = (scope: string) => {
  if (scope.includes("全部")) return "red";
  if (scope.includes("本部门及以下")) return "orange";
  if (scope.includes("本部门")) return "blue";
  if (scope.includes("仅本人")) return "green";
  return "default";
};

const handleLogout = async () => {
  try {
    await logout();
  } catch (e) {}
  localStorage.removeItem("token");
  localStorage.removeItem("deptList");
  router.push("/login");
};
</script>

<style scoped>
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #001529;
  padding: 0 24px;
}
.logo {
  color: white;
  font-size: 18px;
  font-weight: bold;
}
.user-info {
  color: white;
}
.content {
  padding: 24px;
  background: #f0f2f5;
}
</style>
