<template>
  <div class="page-container" ref="pageContainerRef" style="position: relative; display: flex; flex-direction: column; height: 100%; overflow: hidden;">
    <div style="flex: 1; overflow-y: auto; padding: 12px;">
      <a-card title="文章列表" :bordered="false">
        <template #extra>
          <a-space>
            <a-button type="primary" @click="openAiModal">
              <template #icon>
                <RobotOutlined/>
              </template>
              AI 生成文章
            </a-button>
            <a-input-search
                v-model:value="searchTitle"
                placeholder="输入文章标题查询"
                enter-button
                @search="handleSearch"
                style="width: 300px"
            />
            <a-button @click="fetchList">刷新</a-button>
          <a-popconfirm
              title="确定要批量通过选中的文章吗？"
              @confirm="handleBatchAudit(1)"
              :disabled="selectedRowKeys.length === 0"
          >
            <a-button
                type="primary"
                :disabled="selectedRowKeys.length === 0"
            >
              <template #icon>
                <CheckOutlined/>
              </template>
              批量通过
            </a-button>
          </a-popconfirm>
          <a-popconfirm
              title="确定要批量驳回选中的文章吗？"
              @confirm="handleBatchAudit(2)"
              :disabled="selectedRowKeys.length === 0"
          >
            <a-button
                type="primary"
                :style="{'background-color': selectedRowKeys.length === 0 ? '' :'#faad14', 'border-color': selectedRowKeys.length === 0 ? '' : '#faad14'}"
                :disabled="selectedRowKeys.length === 0"
            >
              <template #icon>
                <CloseOutlined/>
              </template>
              批量驳回
            </a-button>
          </a-popconfirm>
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
              <template #icon>
                <DeleteOutlined/>
              </template>
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
          <template v-if="column.key === 'status'">
            <a-tag :color="getStatusColor(record.status)">{{ getStatusText(record.status) }}</a-tag>
          </template>
          <template v-if="column.key === 'sourceType'">
            <a-tag :color="getSourceColor(record.sourceType)">{{ record.sourceType || 'CRAWLER' }}</a-tag>
          </template>
          <template v-if="column.key === 'url'">
            <a v-if="record.url" :href="record.url" target="_blank">{{
                record.url
              }}</a>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <!-- 通过按钮：待审核(0)或已驳回(2)时显示 -->
              <a-popconfirm
                  v-if="(record.status === 0 || record.status === 2) && (hasPermission('crawler:article:audit') || userStore.userInfo?.userId === 1)"
                  title="确定审核通过吗？"
                  @confirm="handleAudit(record, 1)"
              >
                <a-button type="link" size="small" style="color: #52c41a">
                  <CheckOutlined/>
                  通过
                </a-button>
              </a-popconfirm>
              <!-- 驳回按钮：仅已发布(1)时显示 -->
              <a-popconfirm
                  v-if="record.status === 1 && (hasPermission('crawler:article:audit') || userStore.userInfo?.userId === 1)"
                  title="确定驳回吗？"
                  @confirm="handleAudit(record, 2)"
              >
                <a-button type="link" size="small" style="color: #faad14">
                  <CloseOutlined/>
                  驳回
                </a-button>
              </a-popconfirm>

              <a-button type="link" size="small" @click="openDetail(record.id)">
                <EyeOutlined/>
                详情
              </a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>
                  <DeleteOutlined/>
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>
    </div> <!-- Close internal scroll -->

    <a-drawer v-model:open="drawerVisible" title="文章详情" width="60%">
      <div v-if="currentArticle" class="article-detail">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="文章标题">{{
              currentArticle.title
            }}
          </a-descriptions-item>
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
            }}
          </a-descriptions-item>
          <a-descriptions-item label="发布时间">{{
              currentArticle.publishTime || "未知"
            }}
          </a-descriptions-item>
          <a-descriptions-item label="来源站点">{{
              currentArticle.sourceSite || "未知"
            }}
          </a-descriptions-item>
          <a-descriptions-item label="摘要">
            <div class="summary">{{ currentArticle.summary || "无" }}</div>
          </a-descriptions-item>
          <a-descriptions-item label="正文">
            <div class="content" v-html="renderMarkdown(currentArticle.content)"></div>
          </a-descriptions-item>
        </a-descriptions>
      </div>
    </a-drawer>


    <!-- AI 生成文章弹窗 -->
    <a-modal
        v-model:open="aiModalVisible"
        title="AI 生成文章"
        width="80%"
        centered
        wrapClassName="ai-modal-offset"
        :footer="null"
        :destroyOnClose="true"
    >
      <a-steps :current="aiStep" style="margin-bottom: 24px">
        <a-step title="输入主题"/>
        <a-step title="预览内容"/>
        <a-step title="确认保存"/>
      </a-steps>

      <!-- Step 1: 输入主题 -->
      <div v-if="aiStep === 0">
        <a-form :model="aiForm" layout="vertical">
          <a-form-item label="文章主题" required>
            <a-input v-model:value="aiForm.topic" placeholder="例如：Spring Boot 入门教程"/>
          </a-form-item>
          <a-form-item label="关键词">
            <a-input v-model:value="aiForm.keywords" placeholder="例如：Java, 微服务, REST API（可选）"/>
          </a-form-item>
          <a-form-item label="文章风格">
            <a-select v-model:value="aiForm.style" style="width: 200px">
              <a-select-option value="技术教程">技术教程</a-select-option>
              <a-select-option value="新闻报道">新闻报道</a-select-option>
              <a-select-option value="产品评测">产品评测</a-select-option>
              <a-select-option value="观点评论">观点评论</a-select-option>
            </a-select>
          </a-form-item>
        </a-form>
        <div style="text-align: right; margin-top: 24px">
          <a-button @click="aiModalVisible = false">取消</a-button>
          <a-button type="primary" :loading="aiGenerating" @click="handleGenerate" style="margin-left: 8px">
            生成文章
          </a-button>
        </div>
      </div>

      <!-- Step 2: 预览内容 -->
      <div v-if="aiStep === 1">
        <a-form :model="aiResult" layout="vertical">
          <a-form-item label="标题">
            <a-input v-model:value="aiResult.title"/>
          </a-form-item>
          <a-form-item label="摘要">
            <a-textarea v-model:value="aiResult.summary" :rows="3"/>
          </a-form-item>
          <a-form-item label="正文">
            <a-textarea v-model:value="aiResult.content" :rows="15"/>
          </a-form-item>
          <a-form-item label="修改意见（用于修正功能）">
            <a-input v-model:value="correctionText" placeholder="输入修改意见后点击'修正'"/>
          </a-form-item>
        </a-form>
        <div style="text-align: right; margin-top: 24px">
          <a-button @click="aiStep = 0">返回</a-button>
          <a-button :loading="aiGenerating" @click="handleRegenerate" style="margin-left: 8px">重新生成</a-button>
          <a-button :loading="aiGenerating" @click="handleOptimize" style="margin-left: 8px">优化</a-button>
          <a-button :loading="aiGenerating" @click="handleCorrect" :disabled="!correctionText" style="margin-left: 8px">
            修正
          </a-button>
          <a-button type="primary" @click="aiStep = 2" style="margin-left: 8px">下一步</a-button>
        </div>
      </div>

      <!-- Step 3: 确认保存 -->
      <div v-if="aiStep === 2">
        <a-descriptions bordered :column="1">
          <a-descriptions-item label="标题">{{ aiResult.title }}</a-descriptions-item>
          <a-descriptions-item label="摘要">{{ aiResult.summary }}</a-descriptions-item>
          <a-descriptions-item label="正文预览">
            <div style="max-height: 300px; overflow-y: auto" v-html="renderMarkdown(aiResult.content)"></div>
          </a-descriptions-item>
        </a-descriptions>
        <a-form :model="saveForm" layout="vertical" style="margin-top: 24px">
          <a-form-item label="作者标注">
            <a-radio-group v-model:value="saveForm.authorType">
              <a-radio value="SELF">使用当前登录用户</a-radio>
              <a-radio value="AI">标注为 AI 自动生成</a-radio>
            </a-radio-group>
          </a-form-item>
        </a-form>
        <a-alert message="保存后文章将进入待审核状态，审核通过后才会在前台展示。" type="info" show-icon
                 style="margin-top: 16px"/>
        <div style="text-align: right; margin-top: 24px">
          <a-button @click="aiStep = 1">返回修改</a-button>
          <a-button type="primary" :loading="aiSaving" @click="handleSaveAiArticle" style="margin-left: 8px">保存文章
          </a-button>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, onMounted} from "vue";
import {message} from "ant-design-vue";
import {RobotOutlined, CheckOutlined, CloseOutlined, EyeOutlined, DeleteOutlined} from "@ant-design/icons-vue";
import type {TablePaginationConfig} from "ant-design-vue";
import {hasPermission} from "../../../../utils/permission";
import {useUserStore} from "../../../../store/useUser"; // Import store for debugging
import { marked } from "marked";
import hljs from "highlight.js";
import "highlight.js/styles/atom-one-dark.css"; // 引入代码高亮样式

// ... imports

// ... at the start of script setup
const userStore = useUserStore();

onMounted(async () => {
  // 强制刷新用户信息以获取最新权限
  await userStore.getUserInfo();
  fetchList();
});
import {
  fetchGetArticleList,
  fetchGetArticleDetail,
  fetchDeleteArticle,
  fetchAuditArticle,
  fetchBatchAuditArticle,
} from "../../../../api/admin/crawler";


import {
  generateArticle,
  optimizeArticle,
  correctArticle,
  saveAiArticle,
} from "../../../../api/article/ai";

defineOptions({name: "ArticlePage"});

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
  {title: "ID", dataIndex: "id", key: "id", width: 60},
  {title: "标题", dataIndex: "title", key: "title", width: 200, ellipsis: true},
  {title: "作者", dataIndex: "author", key: "author", width: 100, ellipsis: true},
  {title: "来源", dataIndex: "sourceType", key: "sourceType", width: 80},
  {title: "状态", dataIndex: "status", key: "status", width: 80},
  {title: "发布时间", dataIndex: "publishTime", key: "publishTime", width: 150},
  {title: "入库时间", dataIndex: "createTime", key: "createTime", width: 150},
  {title: "操作", key: "action", width: 280, fixed: "right"},
];

// 详情
const drawerVisible = ref(false);
const currentArticle = ref<any>(null);

// AI 生成相关
const aiModalVisible = ref(false);
const aiStep = ref(1);
// const pageContainerRef = ref(null);
const aiConfig = ref({
  provider: '',
  apiKey: '',
});
const aiGenerating = ref(false);
const aiSaving = ref(false);
const correctionText = ref("");

const aiForm = reactive({
  topic: "",
  keywords: "",
  style: "技术教程",
});

const aiResult = reactive({
  title: "",
  summary: "",
  content: "",
});

const saveForm = reactive({
  authorType: "SELF",
});

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

const handleAudit = async (record: any, status: number) => {
  try {
    await fetchAuditArticle({id: record.id, status});
    message.success(status === 1 ? "审核通过" : "已驳回");
    fetchList();
  } catch (e: any) {
    message.error("操作失败");
  }
};

const handleBatchDelete = async () => {
  if (!selectedRowKeys.value.length) return;
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

const handleBatchAudit = async (status: number) => {
  if (!selectedRowKeys.value.length) return;
  try {
    await fetchBatchAuditArticle({
      ids: selectedRowKeys.value,
      status,
    });
    message.success(status === 1 ? "批量通过成功" : "批量驳回成功");
    selectedRowKeys.value = [];
    fetchList();
  } catch (e: any) {
    message.error(e.message || "批量审核失败");
  }
};

// ========== AI 生成相关 ==========
const openAiModal = () => {
  aiStep.value = 0;
  aiForm.topic = "";
  aiForm.keywords = "";
  aiForm.style = "技术教程";
  aiResult.title = "";
  aiResult.summary = "";
  aiResult.content = "";
  correctionText.value = "";
  saveForm.authorType = "SELF";
  aiModalVisible.value = true;
};

const handleGenerate = async () => {
  if (!aiForm.topic) {
    message.warning("请输入文章主题");
    return;
  }
  aiGenerating.value = true;
  try {
    const res: any = await generateArticle({
      topic: aiForm.topic,
      keywords: aiForm.keywords,
      style: aiForm.style,
    });
    aiResult.title = res.title || "";
    aiResult.summary = res.summary || "";
    aiResult.content = res.content || "";
    aiStep.value = 1;
    message.success("文章生成成功");
  } catch (e: any) {
    message.error(e.message || "生成失败");
  } finally {
    aiGenerating.value = false;
  }
};

const handleRegenerate = async () => {
  await handleGenerate();
};

const handleOptimize = async () => {
  if (!aiResult.content) {
    message.warning("请先生成文章");
    return;
  }
  aiGenerating.value = true;
  try {
    const res: any = await optimizeArticle({
      title: aiResult.title,
      content: aiResult.content,
    });
    aiResult.title = res.title || aiResult.title;
    aiResult.summary = res.summary || aiResult.summary;
    aiResult.content = res.content || aiResult.content;
    message.success("优化成功");
  } catch (e: any) {
    message.error(e.message || "优化失败");
  } finally {
    aiGenerating.value = false;
  }
};

const handleCorrect = async () => {
  if (!correctionText.value) {
    message.warning("请输入修改意见");
    return;
  }
  aiGenerating.value = true;
  try {
    const res: any = await correctArticle({
      title: aiResult.title,
      content: aiResult.content,
      correction: correctionText.value,
    });
    aiResult.title = res.title || aiResult.title;
    aiResult.summary = res.summary || aiResult.summary;
    aiResult.content = res.content || aiResult.content;
    correctionText.value = "";
    message.success("修正成功");
  } catch (e: any) {
    message.error(e.message || "修正失败");
  } finally {
    aiGenerating.value = false;
  }
};

const handleSaveAiArticle = async () => {
  aiSaving.value = true;
  try {
    await saveAiArticle({
      title: aiResult.title,
      content: aiResult.content,
      summary: aiResult.summary,
      authorType: saveForm.authorType,
    });
    message.success("保存成功，文章已进入待审核状态");
    aiModalVisible.value = false;
    fetchList();
  } catch (e: any) {
    message.error(e.message || "保存失败");
  } finally {
    aiSaving.value = false;
  }
};

const getStatusColor = (status: number) => {
  switch (status) {
    case 0:
      return "orange";
    case 1:
      return "green";
    case 2:
      return "red";
    default:
      return "default";
  }
};

const getStatusText = (status: number) => {
  switch (status) {
    case 0:
      return "待审核";
    case 1:
      return "已发布";
    case 2:
      return "已驳回";
    default:
      return "未知";
  }
};

const getSourceColor = (sourceType: string) => {
  switch (sourceType) {
    case "AI":
      return "purple";
    case "MANUAL":
      return "blue";
    default:
      return "default";
  }
};

const renderMarkdown = (text: string) => {
  if (!text) return "";
  return marked.parse(text) as string;
};

// 配置 marked 自定义渲染器以支持代码高亮和语言显示
const renderer = new marked.Renderer();
renderer.code = ({ text, lang }: any) => {
  const language = hljs.getLanguage(lang) ? lang : 'plaintext';
  const highlighted = hljs.highlight(text, { language }).value;
  return `<div class="code-block-wrapper">
            <div class="code-block-header">
              <span class="code-lang">${lang || 'text'}</span>
            </div>
            <pre><code class="hljs language-${language}">${highlighted}</code></pre>
          </div>`;
};

marked.use({ renderer });
</script>

<style scoped>
.page-container {
  height: 100%;
}
/* 弹窗偏移：使弹窗视觉上居中于右侧内容区（Sidebar宽度约220px） */
:global(.ai-modal-offset) {
  padding-left: 220px;
}
/* Markdown 内容样式适配 */
:deep(.content), :deep(.ant-descriptions-item-content) {
  font-size: 14px;
  line-height: 1.6;
}

:deep(.code-block-wrapper) {
  position: relative;
  margin: 16px 0;
  background-color: #282c34;
  border-radius: 6px;
  overflow: hidden;
}

:deep(.code-block-header) {
  display: flex;
  justify-content: flex-end;
  padding: 4px 12px;
  background-color: #21252b;
  color: #abb2bf;
  font-size: 12px;
  font-family: monospace;
  border-bottom: 1px solid #3e4451;
}

:deep(pre) {
  margin: 0;
  padding: 12px;
  overflow-x: auto;
  background-color: transparent !important; /* 让 hljs 背景色透明，使用 wrapper 背景 */
}

:deep(code.hljs) {
  font-family: 'Fira Code', 'Consolas', monospace;
  background-color: transparent;
  padding: 0;
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

