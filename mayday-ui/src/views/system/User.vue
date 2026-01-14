<template>
  <div class="page-container">
    <a-card title="用户管理">
      <template #extra>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :dataSource="userList"
        :loading="loading"
        rowKey="id"
        :pagination="{
          pageSize: 10,
          showTotal: (total: number) => `共 ${total} 条`,
        }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === '0' ? 'green' : 'red'">
              {{ record.status === "0" ? "正常" : "禁用" }}
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
                @click="handleAssignRoles(record)"
                >分配角色</a-button
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
        <a-form-item label="用户名" v-if="!formData.id">
          <a-input
            v-model:value="formData.username"
            placeholder="请输入用户名"
          />
        </a-form-item>
        <a-form-item label="密码" v-if="!formData.id">
          <a-input-password
            v-model:value="formData.password"
            placeholder="请输入密码"
          />
        </a-form-item>
        <a-form-item label="新密码" v-if="formData.id">
          <a-input-password
            v-model:value="formData.password"
            placeholder="不修改请留空"
          />
        </a-form-item>
        <a-form-item label="状态">
          <a-radio-group v-model:value="formData.status">
            <a-radio value="0">正常</a-radio>
            <a-radio value="1">禁用</a-radio>
          </a-radio-group>
        </a-form-item>
        <a-form-item label="部门">
          <a-select
            v-model:value="formData.deptIds"
            mode="multiple"
            placeholder="请选择部门"
          >
            <a-select-option
              v-for="dept in deptOptions"
              :key="dept.id"
              :value="dept.id"
            >
              {{ dept.deptName }}
            </a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 分配角色弹窗 -->
    <a-modal
      v-model:open="roleVisible"
      title="分配角色"
      @ok="handleRoleSubmit"
      :confirmLoading="submitting"
    >
      <a-checkbox-group v-model:value="selectedRoleIds" style="width: 100%">
        <a-row>
          <a-col :span="8" v-for="role in roleOptions" :key="role.id">
            <a-checkbox :value="role.id">{{ role.roleName }}</a-checkbox>
          </a-col>
        </a-row>
      </a-checkbox-group>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import request from "../../utils/request";

const loading = ref(false);
const submitting = ref(false);
const userList = ref<any[]>([]);
const deptOptions = ref<any[]>([]);
const roleOptions = ref<any[]>([]);

// 新增/编辑表单
const formVisible = ref(false);
const formData = ref<any>({});
const formTitle = computed(() => (formData.value.id ? "编辑用户" : "新增用户"));

// 分配角色
const roleVisible = ref(false);
const currentUserId = ref<number>();
const selectedRoleIds = ref<number[]>([]);

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 60 },
  { title: "用户名", dataIndex: "username", key: "username", width: 120 },
  { title: "状态", dataIndex: "status", key: "status", width: 80 },
  { title: "操作", key: "action", width: 200 },
];

onMounted(async () => {
  await loadData();
  await loadDepts();
  await loadRoles();
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await request.get("/system/user/list");
    userList.value = (res as any) || [];
  } catch (e) {
    console.error("加载失败:", e);
  } finally {
    loading.value = false;
  }
};

const loadDepts = async () => {
  try {
    // 简化：直接使用固定数据
    deptOptions.value = [
      { id: 1, deptName: "总公司" },
      { id: 2, deptName: "技术部" },
      { id: 3, deptName: "市场部" },
      { id: 4, deptName: "后端组" },
      { id: 5, deptName: "前端组" },
    ];
  } catch (e) {}
};

const loadRoles = async () => {
  try {
    const res = await request.get("/system/role/all");
    roleOptions.value = (res as any) || [];
  } catch (e) {}
};

const handleAdd = () => {
  formData.value = { status: "0", deptIds: [] };
  formVisible.value = true;
};

const handleEdit = async (record: any) => {
  try {
    const res = await request.get(`/system/user/${record.id}`);
    formData.value = { ...(res as any) };
    formVisible.value = true;
  } catch (e) {
    message.error("获取详情失败");
  }
};

const handleSubmit = async () => {
  submitting.value = true;
  try {
    if (formData.value.id) {
      await request.put(`/system/user/${formData.value.id}`, formData.value);
      message.success("更新成功");
    } else {
      await request.post("/system/user", formData.value);
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
    await request.delete(`/system/user/${id}`);
    message.success("删除成功");
    await loadData();
  } catch (e: any) {
    message.error(e.message || "删除失败");
  }
};

const handleAssignRoles = async (record: any) => {
  currentUserId.value = record.id;
  try {
    const res = await request.get(`/system/user/${record.id}`);
    selectedRoleIds.value = (res as any).roleIds || [];
    roleVisible.value = true;
  } catch (e) {
    message.error("获取角色失败");
  }
};

const handleRoleSubmit = async () => {
  submitting.value = true;
  try {
    await request.put(`/system/user/${currentUserId.value}/roles`, {
      roleIds: selectedRoleIds.value,
    });
    message.success("分配成功");
    roleVisible.value = false;
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
