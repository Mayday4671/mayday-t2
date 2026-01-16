<template>
  <div class="page-container">
    <a-card title="角色管理">
      <template #extra>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :dataSource="roleList"
        :loading="loading"
        rowKey="id"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'dataScope'">
            <a-tag :color="getDataScopeColor(record.dataScope)">
              {{ getDataScopeText(record.dataScope) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)"
                >编辑</a-button
              >
              <a-button
                type="link"
                size="small"
                @click="handleAssignMenus(record)"
                >分配权限</a-button
              >
              <a-popconfirm
                title="确定删除？"
                @confirm="handleDelete(record.id)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:open="formVisible"
      :title="formTitle"
      @ok="handleSubmit"
      :confirmLoading="submitting"
    >
      <a-form :model="formData" layout="vertical">
        <a-form-item label="角色标识" v-if="!formData.id">
          <a-input
            v-model:value="formData.roleKey"
            placeholder="如：admin, manager"
          />
        </a-form-item>
        <a-form-item label="角色名称">
          <a-input
            v-model:value="formData.roleName"
            placeholder="如：超级管理员"
          />
        </a-form-item>
        <a-form-item label="数据权限">
          <a-select
            v-model:value="formData.dataScope"
            placeholder="选择数据权限范围"
          >
            <a-select-option value="1">全部数据</a-select-option>
            <a-select-option value="2">自定义数据</a-select-option>
            <a-select-option value="3">本部门数据</a-select-option>
            <a-select-option value="4">本部门及以下</a-select-option>
            <a-select-option value="5">仅本人数据</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配权限弹窗 -->
    <a-modal
      v-model:open="menuVisible"
      title="分配菜单权限"
      @ok="handleMenuSubmit"
      :confirmLoading="submitting"
      width="600px"
    >
      <a-tree
        v-model:checkedKeys="selectedMenuIds"
        :tree-data="menuTree"
        checkable
        :field-names="{ title: 'menuName', key: 'id', children: 'children' }"
        default-expand-all
      />
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import request from "../../../utils/request";

const loading = ref(false);
const submitting = ref(false);
const roleList = ref<any[]>([]);
const menuTree = ref<any[]>([]);

// 新增/编辑表单
const formVisible = ref(false);
const formData = ref<any>({});
const formTitle = computed(() => (formData.value.id ? "编辑角色" : "新增角色"));

// 分配菜单
const menuVisible = ref(false);
const currentRoleId = ref<number>();
const selectedMenuIds = ref<number[]>([]);

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 60 },
  { title: "角色标识", dataIndex: "roleKey", key: "roleKey", width: 120 },
  { title: "角色名称", dataIndex: "roleName", key: "roleName", width: 120 },
  { title: "数据权限", dataIndex: "dataScope", key: "dataScope", width: 150 },
  { title: "操作", key: "action", width: 200 },
];

const getDataScopeText = (scope: string) => {
  const map: Record<string, string> = {
    "1": "全部数据",
    "2": "自定义数据",
    "3": "本部门数据",
    "4": "本部门及以下",
    "5": "仅本人数据",
  };
  return map[scope] || "未知";
};

const getDataScopeColor = (scope: string) => {
  const map: Record<string, string> = {
    "1": "red",
    "2": "purple",
    "3": "blue",
    "4": "orange",
    "5": "green",
  };
  return map[scope] || "default";
};

onMounted(async () => {
  await loadData();
  await loadMenuTree();
});

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await request.get("/system/role/list");
    roleList.value = res.list || [];
  } catch (e) {
    console.error("加载失败:", e);
  } finally {
    loading.value = false;
  }
};

const loadMenuTree = async () => {
  try {
    const res: any = await request.get("/menu/tree");
    menuTree.value = res || [];
  } catch (e) {}
};

const handleAdd = () => {
  formData.value = { dataScope: "5" };
  formVisible.value = true;
};

const handleEdit = async (record: any) => {
  try {
    const res: any = await request.get(`/system/role/${record.id}`);
    formData.value = { ...res };
    formVisible.value = true;
  } catch (e) {
    message.error("获取详情失败");
  }
};

const handleSubmit = async () => {
  submitting.value = true;
  try {
    if (formData.value.id) {
      await request.put(`/system/role/${formData.value.id}`, formData.value);
      message.success("更新成功");
    } else {
      await request.post("/system/role", formData.value);
      message.success("新增成功");
    }
    formVisible.value = false;
    await loadData();
  } catch (e: any) {
    message.error(e.message || "操作失败");
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await request.delete(`/system/role/${id}`);
    message.success("删除成功");
    await loadData();
  } catch (e: any) {
    message.error(e.message || "删除失败");
  }
};

const handleAssignMenus = async (record: any) => {
  currentRoleId.value = record.id;
  try {
    const res: any = await request.get(`/system/role/${record.id}`);
    selectedMenuIds.value = res.menuIds || [];
    menuVisible.value = true;
  } catch (e) {
    message.error("获取菜单失败");
  }
};

const handleMenuSubmit = async () => {
  submitting.value = true;
  try {
    await request.put(`/system/role/${currentRoleId.value}/menus`, {
      menuIds: selectedMenuIds.value,
    });
    message.success("分配成功");
    menuVisible.value = false;
  } catch (e: any) {
    message.error(e.message || "分配失败");
  } finally {
    submitting.value = false;
  }
};
</script>

<style scoped>
.page-container {
  padding: 8px;
}
</style>
