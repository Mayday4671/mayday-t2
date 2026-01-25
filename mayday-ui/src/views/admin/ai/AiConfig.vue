<template>
  <div class="page-container">
    <a-card title="AI 路由配置">
      <template #extra>
        <a-button type="primary" @click="handleAdd" v-if="hasPermission('ai:config:add')">
          <template #icon><PlusOutlined /></template>
          新增配置
        </a-button>
      </template>

      <!-- 搜索表单 -->
      <a-form layout="inline" style="margin-bottom: 24px">
        <a-form-item label="场景">
          <a-input v-model:value="queryParams.sceneCode" placeholder="例如 article_generate" allowClear />
        </a-form-item>
        <a-form-item label="提供商">
          <a-input v-model:value="queryParams.provider" placeholder="例如 openai" allowClear />
        </a-form-item>
        <a-form-item label="模型">
           <a-input v-model:value="queryParams.modelName" placeholder="例如 gpt-4o" allowClear />
        </a-form-item>
        <a-form-item label="状态">
          <a-select v-model:value="queryParams.enabled" placeholder="全部" style="width: 100px" allowClear>
            <a-select-option :value="1">启用</a-select-option>
            <a-select-option :value="0">停用</a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item>
          <a-button type="primary" @click="handleQuery">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button style="margin-left: 8px" @click="resetQuery">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
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
          <template v-if="column.key === 'enabled'">
            <a-switch 
                :checked="record.enabled === 1" 
                checked-children="启" 
                un-checked-children="停"
                :disabled="!hasPermission('ai:config:edit')"
                @change="(val: any) => handleStatusChange(record, val)"
            />
          </template>
          <template v-if="column.key === 'params'">
             <a-tag>Temp: {{ record.temperature }}</a-tag>
             <a-tag v-if="record.maxTokens">Max: {{ record.maxTokens }}</a-tag>
          </template>
           <template v-if="column.key === 'route'">
             <a-tag color="blue">P{{ record.priority }}</a-tag>
             <a-tag color="orange">W{{ record.weight }}</a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)" v-if="hasPermission('ai:config:edit')">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm
                title="确定删除？"
                @confirm="handleDelete(record.id)"
                v-if="hasPermission('ai:config:remove')"
              >
                <a-button type="link" size="small" danger>
                   <template #icon><DeleteOutlined /></template>
                   删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="formVisible"
      :title="formTitle"
      @ok="handleSubmit"
      :confirmLoading="submitting"
      width="700px"
    >
      <a-form :model="formData" layout="vertical">
        <a-row :gutter="16">
            <a-col :span="12">
                <a-form-item label="业务场景 (Scene Code)" required>
                  <a-input v-model:value="formData.sceneCode" placeholder="例如 article_generate" />
                </a-form-item>
            </a-col>
            <a-col :span="12">
                 <a-form-item label="租户ID (Tenant ID)" required>
                  <a-input v-model:value="formData.tenantId" placeholder="默认 *" />
                </a-form-item>
            </a-col>
        </a-row>

        <a-divider orientation="left">模型参数</a-divider>

        <a-row :gutter="16">
             <a-col :span="8">
                 <a-form-item label="选择密钥 (Key)" required>
                  <a-select 
                      v-model:value="formData.keyId" 
                      style="width: 100%" 
                      placeholder="请选择配置好的密钥" 
                      @change="handleKeyChange"
                      show-search
                      option-filter-prop="label"
                  >
                        <a-select-option 
                            v-for="key in keyList" 
                            :key="key.id" 
                            :value="key.id"
                            :label="key.name"
                        >
                            {{ key.name }} ({{ key.provider }})
                        </a-select-option>
                  </a-select>
                </a-form-item>
            </a-col>
            <a-col :span="8">
                <a-form-item label="提供商" required>
                  <a-input v-model:value="formData.provider" placeholder="自动填充，可修改" />
                </a-form-item>
            </a-col>
            <a-col :span="8">
                 <a-form-item label="模型名称" required>
                  <a-input v-model:value="formData.modelName" placeholder="自动填充，可修改" />
                </a-form-item>
            </a-col>
        </a-row>

         <a-form-item label="Base URL (可选)">
             <a-input v-model:value="formData.baseUrl" placeholder="https://api.openai.com/v1" />
         </a-form-item>

        <a-row :gutter="16">
            <a-col :span="8">
                <a-form-item label="温度 (Temperature)" required>
                  <a-input-number v-model:value="formData.temperature" :min="0" :max="2" :step="0.1" style="width: 100%"/>
                </a-form-item>
            </a-col>
            <a-col :span="8">
                 <a-form-item label="最大 Tokens">
                  <a-input-number v-model:value="formData.maxTokens" style="width: 100%" />
                </a-form-item>
            </a-col>
             <a-col :span="8">
                 <a-form-item label="超时 (ms)">
                  <a-input-number v-model:value="formData.timeoutMs" style="width: 100%" />
                </a-form-item>
            </a-col>
        </a-row>

        <a-divider orientation="left">路由策略</a-divider>
         <a-row :gutter="16">
            <a-col :span="8">
                <a-form-item label="优先级 (Priority)" required tooltip="越小越优先">
                  <a-input-number v-model:value="formData.priority" style="width: 100%"/>
                </a-form-item>
            </a-col>
            <a-col :span="8">
                 <a-form-item label="权重 (Weight)" required tooltip="同优先级内按权重轮询">
                  <a-input-number v-model:value="formData.weight" style="width: 100%" />
                </a-form-item>
            </a-col>
             <a-col :span="8">
                 <a-form-item label="状态" required>
                    <a-radio-group v-model:value="formData.enabled">
                        <a-radio :value="1">启用</a-radio>
                        <a-radio :value="0">停用</a-radio>
                    </a-radio-group>
                </a-form-item>
            </a-col>
        </a-row>
        
        <a-form-item label="备注">
          <a-input v-model:value="formData.remark" />
        </a-form-item>

      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed, reactive } from "vue";
import { PlusOutlined, SearchOutlined, ReloadOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import { listAiConfig, addAiConfig, updateAiConfig, deleteAiConfig, switchAiConfigStatus } from "../../../api/ai/config";
import { hasPermission } from "../../../utils/permission";

const loading = ref(false);
const submitting = ref(false);
const dataList = ref<any[]>([]);

const queryParams = reactive({
  sceneCode: "",
  provider: "",
  modelName: "",
  enabled: undefined,
  pageNum: 1,
  pageSize: 10
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
});

const formVisible = ref(false);
const formData = ref<any>({});
const formTitle = computed(() => (formData.value.id ? "编辑配置" : "新增配置"));

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 60 },
  { title: "场景", dataIndex: "sceneCode", key: "sceneCode", width: 120 },
  { title: "提供商", dataIndex: "provider", key: "provider", width: 100 },
  { title: "模型", dataIndex: "modelName", key: "modelName", width: 150 },
  { title: "参数", key: "params", width: 150 },
  { title: "Key ID", dataIndex: "keyId", key: "keyId", width: 80 },
  { title: "路由(P/W)", key: "route", width: 120 },
  { title: "状态", key: "enabled", width: 80 },
  { title: "备注", dataIndex: "remark", key: "remark" },
  { title: "操作", key: "action", width: 150 },
];

onMounted(() => {
  loadData();
});

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await listAiConfig(queryParams);
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
  queryParams.sceneCode = "";
  queryParams.provider = "";
  queryParams.modelName = "";
  queryParams.enabled = undefined;
  handleQuery();
};

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current;
  queryParams.pageSize = pag.pageSize;
  loadData();
};


// ... imports
import { listAiKey } from "../../../api/ai/key";

// ... loading, submitting, etc.

const keyList = ref<any[]>([]);

const loadKeys = async () => {
    try {
        const res: any = await listAiKey({ pageNum: 1, pageSize: 100, status: 'ACTIVE' }); // Load active keys
        if (res && res.records) {
            keyList.value = res.records;
        }
    } catch (e) {
        console.error("加载密钥失败", e);
    }
};

const handleKeyChange = (keyId: number) => {
    const key = keyList.value.find(k => k.id === keyId);
    if (key) {
        formData.value.provider = key.provider;
        // 尝试自动填充模型名称为 Key 的名称（用户可后续修改）
        formData.value.modelName = key.name;
    }
};

// ... other methods

const handleAdd = () => {
  formData.value = {
      tenantId: "*",
      temperature: 0.7,
      timeoutMs: 30000,
      priority: 100,
      weight: 100,
      enabled: 1
  };
  formVisible.value = true;
  loadKeys();
};

const handleEdit = (record: any) => {
  formData.value = { ...record };
  formVisible.value = true;
  loadKeys();
};

const handleSubmit = async () => {
  submitting.value = true;
  try {
    if (formData.value.id) {
       await updateAiConfig(formData.value);
       message.success("更新成功");
    } else {
       await addAiConfig(formData.value);
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
    await deleteAiConfig(id);
    message.success("删除成功");
    loadData();
  } catch (e: any) {
    message.error(e.message || "删除失败");
  }
};

// ... handleDelete, handleStatusChange

const handleStatusChange = async (record: any, checked: boolean) => {
   try {
       await switchAiConfigStatus(record.id, checked ? 1 : 0);
       record.enabled = checked ? 1 : 0;
       message.success("操作成功");
   } catch(e: any) {
       message.error("操作失败");
       // revert visual state if needed, but table refresh will fix it often
   }
}
</script>

<style scoped>
.page-container {
  padding: 8px;
}
</style>
