<template>
  <div class="page-container">
    <a-card title="AI 调用日志">

      <!-- 搜索表单 -->
      <a-form layout="inline" style="margin-bottom: 24px">
        <a-form-item label="Request ID">
          <a-input v-model:value="queryParams.requestId" placeholder="全文匹配" allowClear />
        </a-form-item>
        <a-form-item label="场景">
          <a-input v-model:value="queryParams.sceneCode" placeholder="例如 article_generate" allowClear />
        </a-form-item>
        <a-form-item label="提供商">
          <a-input v-model:value="queryParams.provider" placeholder="例如 openai" allowClear />
        </a-form-item>
        <a-form-item label="结果">
          <a-select v-model:value="queryParams.success" placeholder="全部" style="width: 100px" allowClear>
            <a-select-option :value="1">成功</a-select-option>
            <a-select-option :value="0">失败</a-select-option>
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
          <template v-if="column.key === 'success'">
            <a-tag :color="record.success ? 'green' : 'red'">
              {{ record.success ? "成功" : "失败" }}
            </a-tag>
          </template>
           <template v-if="column.key === 'latency'">
              {{ record.latencyMs }} ms
           </template>
           <template v-if="column.key === 'tokens'">
              IN:{{ record.promptTokens || 0 }} / OUT:{{ record.completionTokens || 0 }}
           </template>
          <template v-if="column.key === 'createTime'">
             {{ formatTime(record.createTime) }}
          </template>
           <template v-if="column.key === 'action'">
             <a-button type="link" size="small" @click="handleDetail(record)">
                <template #icon><EyeOutlined /></template>
                详情
             </a-button>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="detailVisible"
      title="日志详情"
      :footer="null"
      width="600px"
    >
        <a-descriptions bordered :column="1">
            <a-descriptions-item label="Request ID">{{ detailData.requestId }}</a-descriptions-item>
            <a-descriptions-item label="场景 (Scene)">{{ detailData.sceneCode }}</a-descriptions-item>
            <a-descriptions-item label="提供商 (Provider)">{{ detailData.provider }}</a-descriptions-item>
            <a-descriptions-item label="模型 (Model)">{{ detailData.modelName }}</a-descriptions-item>
            <a-descriptions-item label="耗时 (Latency)">{{ detailData.latencyMs }} ms</a-descriptions-item>
            <a-descriptions-item label="结果">
                 <a-tag :color="detailData.success ? 'green' : 'red'">
                    {{ detailData.success ? "成功" : "失败" }}
                 </a-tag>
            </a-descriptions-item>
            <a-descriptions-item label="错误码" v-if="!detailData.success">{{ detailData.errorCode }}</a-descriptions-item>
            <a-descriptions-item label="错误信息" v-if="!detailData.success">{{ detailData.errorMsg }}</a-descriptions-item>
            
            <a-descriptions-item label="Prompt Tokens">{{ detailData.promptTokens }}</a-descriptions-item>
            <a-descriptions-item label="Completion Tokens">{{ detailData.completionTokens }}</a-descriptions-item>
            <a-descriptions-item label="记录时间">{{ formatTime(detailData.createTime) }}</a-descriptions-item>
        </a-descriptions>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from "vue";
import { SearchOutlined, ReloadOutlined, EyeOutlined } from "@ant-design/icons-vue";
import { listAiCallLog } from "../../../api/ai/log";
import dayjs from "dayjs";

const loading = ref(false);
const dataList = ref<any[]>([]);

const queryParams = reactive({
  requestId: "",
  sceneCode: "",
  provider: "",
  success: undefined,
  pageNum: 1,
  pageSize: 10
});

const pagination = reactive({
  current: 1,
  pageSize: 10,
  total: 0,
  showTotal: (total: number) => `共 ${total} 条`,
});

const detailVisible = ref(false);
const detailData = ref<any>({});

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 60 },
  { title: "Request ID", dataIndex: "requestId", key: "requestId", width: 150 },
  { title: "场景", dataIndex: "sceneCode", key: "sceneCode", width: 120 },
  { title: "模型", dataIndex: "modelName", key: "modelName", width: 120 },
  { title: "结果", key: "success", width: 80 },
  { title: "耗时", key: "latency", width: 80 },
  { title: "Tokens", key: "tokens", width: 120 },
  { title: "时间", dataIndex: "createTime", key: "createTime", width: 160 },
  { title: "操作", key: "action", width: 80 },
];

onMounted(() => {
  loadData();
});

const loadData = async () => {
  loading.value = true;
  try {
    const res: any = await listAiCallLog(queryParams);
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
  queryParams.requestId = "";
  queryParams.sceneCode = "";
  queryParams.provider = "";
  queryParams.success = undefined;
  handleQuery();
};

const handleTableChange = (pag: any) => {
  queryParams.pageNum = pag.current;
  queryParams.pageSize = pag.pageSize;
  loadData();
};

const handleDetail = (record: any) => {
    detailData.value = record;
    detailVisible.value = true;
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
