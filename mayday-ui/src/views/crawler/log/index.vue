<template>
  <div class="page-container">
    <a-card title="爬虫日志" :bordered="false">
      <template #extra>
        <a-button @click="fetchList">刷新</a-button>
      </template>

      <a-table
        :loading="loading"
        :dataSource="dataList"
        :columns="columns"
        :pagination="pagination"
        rowKey="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'level'">
            <a-tag :color="getLevelColor(record.level)">{{
              record.level
            }}</a-tag>
          </template>
          <template v-if="column.key === 'createTime'">
            {{ record.createTime }}
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import type { TablePaginationConfig } from "ant-design-vue";
import { fetchGetLogList } from "../../../api/crawler";

defineOptions({ name: "LogPage" });

const loading = ref(false);
const dataList = ref<any[]>([]);

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 20,
  total: 0,
  showSizeChanger: true,
});

const columns = [
  { title: "ID", dataIndex: "id", width: 80 },
  { title: "任务ID", dataIndex: "taskId", width: 100 },
  { title: "级别", key: "level", dataIndex: "level", width: 100 },
  { title: "内容", dataIndex: "message", ellipsis: true },
  { title: "时间", key: "createTime", dataIndex: "createTime", width: 180 },
];

onMounted(() => {
  fetchList();
});

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await fetchGetLogList({
      current: pagination.current,
      size: pagination.pageSize,
    });
    dataList.value = res.records || [];
    pagination.total = res.totalRow || 0;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  fetchList();
};

const getLevelColor = (level: string) => {
  if (level === "ERROR") return "red";
  if (level === "WARN") return "orange";
  return "blue";
};
</script>

<style scoped>
.page-container {
  padding: 8px;
}
</style>
