<template>
  <div class="page-container">
    <a-card title="爬取文章管理" :bordered="false">
      <template #extra>
        <a-space>
          <a-input-search
            v-model:value="searchTitle"
            placeholder="输入文章标题查询"
            enter-button
            @search="handleSearch"
            style="width: 300px"
          />
          <a-button @click="fetchList">刷新</a-button>
          <a-popconfirm
            title="确定要批量删除选中文章吗？"
            @confirm="handleBatchDelete"
            :disabled="selectedRowKeys.length === 0"
          >
            <a-button
              type="primary"
              danger
              :disabled="selectedRowKeys.length === 0"
            >
              批量删除
            </a-button>
          </a-popconfirm>
        </a-space>
      </template>

      <a-table
        :loading="loading"
        :dataSource="dataList"
        :columns="columns"
        :pagination="pagination"
        :row-selection="{
          selectedRowKeys: selectedRowKeys,
          onChange: onSelectChange,
        }"
        rowKey="id"
        @change="handleTableChange"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'url'">
            <a v-if="record.url" :href="record.url" target="_blank">{{
              record.url
            }}</a>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="openDetail(record.id)"
                >详情</a-button
              >
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-drawer v-model:open="drawerVisible" title="文章详情" width="60%">
      <div v-if="currentArticle" class="article-detail">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="文章标题">{{
            currentArticle.title
          }}</a-descriptions-item>
          <a-descriptions-item label="文章URL">
            <a
              v-if="currentArticle.url"
              :href="currentArticle.url"
              target="_blank"
              >{{ currentArticle.url }}</a
            >
          </a-descriptions-item>
          <a-descriptions-item label="作者">{{
            currentArticle.author || "未知"
          }}</a-descriptions-item>
          <a-descriptions-item label="发布时间">{{
            currentArticle.publishTime || "未知"
          }}</a-descriptions-item>
          <a-descriptions-item label="来源站点">{{
            currentArticle.sourceSite || "未知"
          }}</a-descriptions-item>
          <a-descriptions-item label="摘要">
            <div class="summary">{{ currentArticle.summary || "无" }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="正文">
            <div class="content" v-html="currentArticle.content"></div>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { message } from "ant-design-vue";
import type { TablePaginationConfig } from "ant-design-vue";
import {
  fetchGetArticleList,
  fetchGetArticleDetail,
  fetchDeleteArticle,
} from "../../../api/crawler";

defineOptions({ name: "ArticlePage" });

const loading = ref(false);
const dataList = ref<any[]>([]);
const selectedRowKeys = ref<number[]>([]);
const searchTitle = ref("");

// 分页
const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showTotal: (total) => `共 ${total} 条`,
});

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 60 },
  {
    title: "标题",
    dataIndex: "title",
    key: "title",
    width: 250,
    ellipsis: true,
  },
  {
    title: "作者",
    dataIndex: "author",
    key: "author",
    width: 120,
    ellipsis: true,
  },
  { title: "来源", dataIndex: "sourceSite", key: "sourceSite", width: 150 },
  {
    title: "发布时间",
    dataIndex: "publishTime",
    key: "publishTime",
    width: 180,
  },
  { title: "URL", dataIndex: "url", key: "url", width: 200, ellipsis: true },
  { title: "入库时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 150, fixed: "right" },
];

// 详情
const drawerVisible = ref(false);
const currentArticle = ref<any>(null);

onMounted(() => {
  fetchList();
});

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await fetchGetArticleList({
      current: pagination.current,
      pageSize: pagination.pageSize,
      title: searchTitle.value,
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

const handleSearch = () => {
  pagination.current = 1;
  fetchList();
};

const onSelectChange = (keys: number[]) => {
  selectedRowKeys.value = keys;
};

const openDetail = async (id: number) => {
  try {
    const res = await fetchGetArticleDetail(id);
    currentArticle.value = res;
    drawerVisible.value = true;
  } catch (e: any) {
    message.error("获取详情失败");
  }
};

const handleDelete = async (record: any) => {
  try {
    await fetchDeleteArticle(record.id);
    message.success("删除成功");
    fetchList();
  } catch (e: any) {
    message.error("删除失败");
  }
};

const handleBatchDelete = async () => {
  if (!selectedRowKeys.value.length) return;
  // Serial delete for simplicity, ideally should have batch API
  try {
    await Promise.all(
      selectedRowKeys.value.map((id) => fetchDeleteArticle(id)),
    );
    message.success("批量删除成功");
    selectedRowKeys.value = [];
    fetchList();
  } catch (e) {
    message.error("部分删除失败");
  }
};
</script>

<style scoped>
.page-container {
  padding: 8px;
}
.article-detail .summary {
  max-height: 100px;
  overflow-y: auto;
  background: #f5f5f5;
  padding: 10px;
  border-radius: 4px;
}
.article-detail .content {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
  border: 1px solid #eee;
  border-radius: 4px;
}
</style>
