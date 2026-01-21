<template>
  <div class="page-container">
    <a-card title="AI 密钥管理">
      <template #extra>
        <a-button type="primary" @click="handleAdd" v-if="hasPermission('ai:key:add')">
          <template #icon><PlusOutlined /></template>
          新增密钥
        </a-button>
      </template>

      <!-- 搜索表单 -->
      <a-form layout="inline" style="margin-bottom: 24px">
        <a-form-item label="提供商">
          <a-input v-model:value="queryParams.provider" placeholder="例如 openai" allowClear />
        </a-form-item>
        <a-form-item label="名称">
          <a-input v-model:value="queryParams.name" placeholder="密钥名称" allowClear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="queryParams.status" placeholder="全部" style="width: 120px" allowClear>
            <a-select-option value="ACTIVE">正常</a-select-option>
            <a-select-option value="DISABLED">禁用</a-select-option>
            <a-select-option value="REVOKED">已撤销</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleQuery">查询</a-button>
          <a-button style="margin-left: 8px" @click="resetQuery">重置</a-button>
        </a-form-item>
      </a-form>

      <a-table
        :columns="columns"
        :dataSource="dataList"
        :loading="loading"
        rowKey="id"
        :pagination="pagination"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">
              {{ getStatusText(record.status) }}
            </a-tag>
          </template>
          <template v-if="column.key === 'createTime'">
             {{ formatTime(record.createTime) }}
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)" v-if="hasPermission('ai:key:edit')">编辑</a-button>
              <a-popconfirm
                v-if="record.status === 'ACTIVE' && hasPermission('ai:key:edit')"
                title="确定禁用该密钥？"
                @confirm="handleDisable(record.id)"
              >
                <a-button type="link" size="small" danger>禁用</a-button>
              </a-popconfirm>
               <a-popconfirm
                v-if="record.status !== 'ACTIVE' && hasPermission('ai:key:edit')"
                title="确定启用该密钥？"
                @confirm="handleEnable(record.id)"
              >
                <a-button type="link" size="small" style="color: #52c41a">启用</a-button>
              </a-popconfirm>
               <a-popconfirm
                title="确定删除？"
                @confirm="handleDelete(record.id)"
                v-if="hasPermission('ai:key:remove')"
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
        <a-form-item label="提供商" required>
          <a-input v-model:value="formData.provider" placeholder="例如 google, openai, deepseek" :disabled="!!formData.id"/>
        </a-form-item>
        <a-form-item label="密钥名称" required>
          <a-input v-model:value="formData.name" placeholder="例如 prod-key-01" />
        </a-form-item>
        
        <a-form-item label="API Key" :required="!formData.id">
          <a-input-password 
            v-model:value="formData.apiKey" 
            :placeholder="formData.id ? '留空则不修改' : '请输入 API Key'" 
          />
          <div v-if="formData.id" style="font-size: 12px; color: #999">注意：出于安全考虑，API Key 不会回显。若需修改请直接输入新 Key。</div>
        </a-form-item>

        <a-form-item label="备注">
          <a-textarea v-model:value="formData.remark" placeholder="用途说明" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { listAiKey, addAiKey, updateAiKey, deleteAiKey, disableAiKey, enableAiKey } from "../../../api/ai/key";
import { hasPermission } from "../../../utils/permission";
import dayjs from "dayjs";

const loading = ref(false);
const submitting = ref(false);
const dataList = ref<any[]>([]);

const queryParams = reactive({
  provider: "",
  name: "",
  status: undefined,
  pageNum: 1,
  pageSize: 10
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
});

// 新增/编辑表单
const formVisible = ref(false);
const formData = ref<any>({});
const formTitle = computed(() => (formData.value.id ? "编辑密钥" : "新增密钥"));

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 60 },
  { title: "提供商", dataIndex: "provider", key: "provider", width: 120 },
  { title: "名称", dataIndex: "name", key: "name", width: 150 },
  { title: "状态", dataIndex: "status", key: "status", width: 100 },
  { title: "备注", dataIndex: "remark", key: "remark" },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 180 },
];

onMounted(() => {
  loadData();
});

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await listAiKey(queryParams);
    if(res && res.records) {
        dataList.value = res.records;
        pagination.total = res.totalRow;
        pagination.current = res.pageNumber;
        pagination.pageSize = res.pageSize;
    } else {
        dataList.value = [];
    }
  } catch (e) {
    console.error("加载失败:", e);
  } finally {
    loading.value = false;
  }
};

const handleQuery = () => {
  queryParams.pageNum = 1;
  loadData();
};

const resetQuery = () => {
  queryParams.provider = "";
  queryParams.name = "";
  queryParams.status = undefined;
  handleQuery();
};

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current;
  queryParams.pageSize = pag.pageSize;
  loadData();
};

const handleAdd = () => {
  formData.value = {};
  formVisible.value = true;
};

const handleEdit = (record: any) => {
  formData.value = { ...record, apiKey: "" }; // Clear apiKey for security
  formVisible.value = true;
};

const handleSubmit = async () => {
  submitting.value = true;
  try {
    if (formData.value.id) {
        // Edit logic
        await updateAiKey(formData.value);
        message.success("更新成功");
    } else {
        // Create logic
        if (!formData.value.provider || !formData.value.name || !formData.value.apiKey) {
             message.error("请填写完整信息");
             return;
        }
        await addAiKey(formData.value);
        message.success("新增成功");
    }
    formVisible.value = false;
    loadData();
  } catch (e: any) {
    message.error(e.message || "操作失败");
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (id: number) => {
  try {
    await deleteAiKey(id);
    message.success("删除成功");
    loadData();
  } catch (e: any) {
    message.error(e.message || "删除失败");
  }
};

const handleDisable = async (id: number) => {
  try {
    await disableAiKey(id);
    message.success("禁用成功");
    loadData();
  } catch (e: any) {
      message.error(e.message || "操作失败");
  }
};

const handleEnable = async (id: number) => {
  try {
    await enableAiKey(id);
    message.success("启用成功");
    loadData();
  } catch (e: any) {
      message.error(e.message || "操作失败");
  }
};

const getStatusColor = (status: string) => {
  switch (status) {
    case "ACTIVE": return "green";
    case "DISABLED": return "red";
    default: return "default";
  }
};

const getStatusText = (status: string) => {
   switch (status) {
    case "ACTIVE": return "正常";
    case "DISABLED": return "禁用";
    case "REVOKED": return "已撤销";
    default: return status;
  }
}

const formatTime = (time: string) => {
    if(!time) return '-';
    return dayjs(time).format('YYYY-MM-DD HH:mm:ss');
}
</script>

<style scoped>
.page-container {
  padding: 8px;
}
</style>
