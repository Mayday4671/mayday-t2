<template>
  <div class="page-container">
    <a-card title="爬虫任务管理" :bordered="false">
      <template #extra>
        <a-space>
          <a-button type="primary" @click="showDialog('add')">
            <template #icon><PlusOutlined /></template>
            新增任务
          </a-button>
          <a-button
            v-if="selectedRowKeys.length > 0"
            danger
            @click="handleBatchDelete"
          >
            批量删除
          </a-button>
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
          <template v-if="column.key === 'crawlType'">
            {{ CRAWL_TYPE_CONFIG[record.crawlType] || record.crawlType }}
          </template>

          <template v-if="column.key === 'status'">
            <div class="status-cell">
              <a-space direction="vertical" size="small">
                <a-tag :color="getStatusColor(record.status)">
                  {{ getStatusText(record.status) }}
                </a-tag>
                <a-progress
                  v-if="record.status === 'RUNNING' && record.totalUrls > 0"
                  :percent="
                    Math.round((record.crawledUrls / record.totalUrls) * 100)
                  "
                  size="small"
                  :show-info="false"
                />
                <span
                  v-if="record.status === 'RUNNING'"
                  style="font-size: 12px; color: #999"
                >
                  {{ record.crawledUrls }}/{{ record.totalUrls }}
                </span>
              </a-space>
            </div>
          </template>

          <template v-if="column.key === 'action'">
            <a-space wrap>
              <a-button
                v-if="!['RUNNING', 'PAUSED'].includes(record.status)"
                type="link"
                size="small"
                style="color: #52c41a"
                @click="handleStartTask(record)"
              >
                启动
              </a-button>

              <a-button
                v-if="record.status === 'RUNNING'"
                type="link"
                size="small"
                style="color: #faad14"
                @click="handlePauseTask(record)"
              >
                暂停
              </a-button>

              <a-button
                v-if="record.status === 'PAUSED'"
                type="link"
                size="small"
                @click="handleResumeTask(record)"
              >
                恢复
              </a-button>

              <a-button
                v-if="['RUNNING', 'PAUSED'].includes(record.status)"
                type="link"
                size="small"
                danger
                @click="handleStopTask(record)"
              >
                停止
              </a-button>

              <a-button
                type="link"
                size="small"
                @click="showDialog('edit', record)"
                :disabled="record.status === 'RUNNING'"
              >
                编辑
              </a-button>

              <a-popconfirm
                title="确定要删除该任务吗？"
                @confirm="handleDelete(record)"
                :disabled="record.status === 'RUNNING'"
              >
                <a-button
                  type="link"
                  size="small"
                  danger
                  :disabled="record.status === 'RUNNING'"
                >
                  删除
                </a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 任务编辑弹窗 -->
    <a-modal
      v-model:open="dialogVisible"
      :title="dialogType === 'add' ? '新增任务' : '编辑任务'"
      width="800px"
      :confirmLoading="submitting"
      @ok="handleSubmit"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="任务名称" name="taskName">
              <a-input
                v-model:value="formData.taskName"
                placeholder="请输入任务名称"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="爬取类型" name="crawlType">
              <a-select
                v-model:value="formData.crawlType"
                placeholder="请选择爬取类型"
              >
                <a-select-option value="ARTICLE">文章</a-select-option>
                <a-select-option value="IMAGE">图片</a-select-option>
                <a-select-option value="BOTH">文章+图片</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item
          label="任务描述"
          name="taskDesc"
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 20 }"
        >
          <a-textarea
            v-model:value="formData.taskDesc"
            :rows="2"
            placeholder="请输入任务描述"
          />
        </a-form-item>

        <a-form-item
          label="起始URL"
          name="startUrls"
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 20 }"
        >
          <a-textarea
            v-model:value="startUrlsText"
            :rows="4"
            placeholder="请输入起始URL，多个URL请换行"
            @blur="handleStartUrlsBlur"
          />
        </a-form-item>

        <a-divider orientation="left">基础配置</a-divider>

        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="最大爬取深度" name="maxDepth">
              <a-input-number
                v-model:value="formData.maxDepth"
                :min="1"
                :max="10"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="站点范围" name="scopeType">
              <a-radio-group v-model:value="formData.scopeType">
                <a-radio value="SITE">仅站内</a-radio>
                <a-radio value="ALL">全站</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="列表翻页数" name="listMaxPages">
              <a-input-number
                v-model:value="formData.listMaxPages"
                :min="1"
                :max="200"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">请求配置</a-divider>

        <a-row :gutter="24">
          <a-col :span="8">
            <a-form-item
              label="请求间隔(ms)"
              name="requestInterval"
              :label-col="{ span: 12 }"
              :wrapper-col="{ span: 12 }"
            >
              <a-input-number
                v-model:value="formData.requestInterval"
                :min="100"
                :max="60000"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="请求超时(ms)"
              name="requestTimeout"
              :label-col="{ span: 12 }"
              :wrapper-col="{ span: 12 }"
            >
              <a-input-number
                v-model:value="formData.requestTimeout"
                :min="1000"
                :max="300000"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="最大重试次数"
              name="maxRetries"
              :label-col="{ span: 12 }"
              :wrapper-col="{ span: 12 }"
            >
              <a-input-number
                v-model:value="formData.maxRetries"
                :min="0"
                :max="10"
                style="width: 100%"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-row :gutter="24">
          <a-col :span="12">
            <a-form-item label="User-Agent" name="userAgent">
              <a-input
                v-model:value="formData.userAgent"
                placeholder="请输入User-Agent"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="Referer" name="referer">
              <a-input
                v-model:value="formData.referer"
                placeholder="请输入Referer"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">高级配置</a-divider>

        <a-row :gutter="24">
          <a-col :span="8">
            <a-form-item
              label="使用代理"
              name="useProxy"
              :label-col="{ span: 12 }"
              :wrapper-col="{ span: 12 }"
            >
              <a-radio-group v-model:value="formData.useProxy">
                <a-radio :value="0">否</a-radio>
                <a-radio :value="1">是</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="随机间隔"
              name="randomInterval"
              :label-col="{ span: 12 }"
              :wrapper-col="{ span: 12 }"
            >
              <a-radio-group v-model:value="formData.randomInterval">
                <a-radio :value="0">否</a-radio>
                <a-radio :value="1">是</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="轮换UA"
              name="rotateUserAgent"
              :label-col="{ span: 12 }"
              :wrapper-col="{ span: 12 }"
            >
              <a-radio-group v-model:value="formData.rotateUserAgent">
                <a-radio :value="0">否</a-radio>
                <a-radio :value="1">是</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
          <a-col :span="8">
            <a-form-item
              label="下载图片"
              name="downloadImages"
              :label-col="{ span: 12 }"
              :wrapper-col="{ span: 12 }"
            >
              <a-radio-group v-model:value="formData.downloadImages">
                <a-radio :value="0">否</a-radio>
                <a-radio :value="1">是</a-radio>
              </a-radio-group>
            </a-form-item>
          </a-col>
        </a-row>

        <a-divider orientation="left">内容提取配置</a-divider>

        <a-form-item
          label="正文选择器"
          name="contentSelector"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 19 }"
        >
          <a-input
            v-model:value="formData.contentSelector"
            placeholder="例如: #content, .article-body"
          />
          <div style="font-size: 12px; color: #999">
            CSS选择器，用于定位正文区域。留空则自动识别。
          </div>
        </a-form-item>

        <a-form-item
          label="图片选择器"
          name="imageSelector"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 19 }"
        >
          <a-input
            v-model:value="formData.imageSelector"
            placeholder="例如: img, .gallery img"
          />
          <div style="font-size: 12px; color: #999">
            CSS选择器，定位正文内图片。留空提取所有img。
          </div>
        </a-form-item>

        <a-form-item
          label="排除选择器"
          name="excludeSelector"
          :label-col="{ span: 4 }"
          :wrapper-col="{ span: 19 }"
        >
          <a-input
            v-model:value="formData.excludeSelector"
            placeholder="例如: .ad, .sidebar"
          />
          <div style="font-size: 12px; color: #999">
            CSS选择器，排除不需要的区域。
          </div>
        </a-form-item>

        <a-form-item
          label="备注"
          name="remark"
          :label-col="{ span: 3 }"
          :wrapper-col="{ span: 20 }"
        >
          <a-textarea
            v-model:value="formData.remark"
            :rows="2"
            placeholder="备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, nextTick, onUnmounted } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { message, Modal } from "ant-design-vue";
import type { TablePaginationConfig } from "ant-design-vue";
import {
  fetchGetTaskList,
  fetchAddTask,
  fetchEditTask,
  fetchDeleteTask,
  fetchStartTask,
  fetchPauseTask,
  fetchResumeTask,
  fetchStopTask,
} from "../../../api/crawler";

defineOptions({ name: "TaskPage" });

const loading = ref(false);
const submitting = ref(false);
const dataList = ref<any[]>([]);
const selectedRowKeys = ref<number[]>([]);

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
    title: "任务名称",
    dataIndex: "taskName",
    key: "taskName",
    width: 180,
    ellipsis: true,
  },
  { title: "类型", dataIndex: "crawlType", key: "crawlType", width: 100 },
  { title: "状态", dataIndex: "status", key: "status", width: 180 },
  {
    title: "成功数",
    dataIndex: "successCount",
    key: "successCount",
    width: 90,
  },
  { title: "错误数", dataIndex: "errorCount", key: "errorCount", width: 90 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 170 },
  { title: "操作", key: "action", width: 280, fixed: "right" },
];

const CRAWL_TYPE_CONFIG: Record<string, string> = {
  ARTICLE: "文章",
  IMAGE: "图片",
  BOTH: "文章+图片",
};

const getStatusColor = (status: string) => {
  const map: Record<string, string> = {
    NOT_STARTED: "default",
    RUNNING: "processing",
    PAUSED: "warning",
    COMPLETED: "success",
    ERROR: "error",
    STOPPED: "default",
  };
  return map[status] || "default";
};

const getStatusText = (status: string) => {
  const map: Record<string, string> = {
    NOT_STARTED: "未启动",
    RUNNING: "运行中",
    PAUSED: "已暂停",
    COMPLETED: "已完成",
    ERROR: "异常",
    STOPPED: "已停止",
  };
  return map[status] || status;
};

// 弹窗相关
const dialogVisible = ref(false);
const dialogType = ref<"add" | "edit">("add");
const formRef = ref();
const startUrlsText = ref("");
const formData = ref<any>({});

const formRules = {
  taskName: [{ required: true, message: "请输入任务名称", trigger: "blur" }],
  crawlType: [{ required: true, message: "请选择爬取类型", trigger: "change" }],
  startUrls: [{ required: true, message: "请输入起始URL", trigger: "blur" }],
};

// 自动刷新
let timer: any = null;

onMounted(() => {
  loadData();
  timer = setInterval(loadData, 5000); // 简单轮询刷新状态
});

onUnmounted(() => {
  if (timer) clearInterval(timer);
});

const loadData = async () => {
  // loading.value = true // 轮询时不显示loading避免闪烁，仅首次或手动刷新时显示? 这里简化逻辑，不显示loading以免干扰操作
  try {
    const res: any = await fetchGetTaskList({
      current: pagination.current,
      size: pagination.pageSize,
    });
    dataList.value = res.records || [];
    pagination.total = res.totalRow || 0;
  } catch (e) {
    console.error(e);
  } finally {
    // loading.value = false
  }
};

const handleTableChange = (pag: TablePaginationConfig) => {
  pagination.current = pag.current;
  pagination.pageSize = pag.pageSize;
  loadData();
};

const onSelectChange = (keys: number[]) => {
  selectedRowKeys.value = keys;
};

// 表单处理
const showDialog = (type: "add" | "edit", record?: any) => {
  dialogType.value = type;
  if (type === "edit" && record) {
    formData.value = { ...record };
    // 处理 startUrls
    if (record.startUrls) {
      if (Array.isArray(record.startUrls)) {
        startUrlsText.value = record.startUrls.join("\n");
        formData.value.startUrls = [...record.startUrls];
      } else if (typeof record.startUrls === "string") {
        try {
          const parsed = JSON.parse(record.startUrls);
          startUrlsText.value = Array.isArray(parsed) ? parsed.join("\n") : "";
          formData.value.startUrls = Array.isArray(parsed) ? parsed : [];
        } catch (e) {
          startUrlsText.value = "";
          formData.value.startUrls = [];
        }
      }
    } else {
      startUrlsText.value = "";
      formData.value.startUrls = [];
    }
  } else {
    // defaults
    formData.value = {
      crawlType: "ARTICLE",
      maxDepth: 3,
      scopeType: "SITE",
      requestInterval: 1000,
      requestTimeout: 30000,
      maxRetries: 3,
      useProxy: 0,
      randomInterval: 0,
      rotateUserAgent: 0,
      listMaxPages: 1,
      downloadImages: 0,
    };
    startUrlsText.value = "";
  }
  dialogVisible.value = true;
  // 清除校验
  nextTick(() => {
    formRef.value?.clearValidate();
  });
};

const handleStartUrlsBlur = () => {
  if (startUrlsText.value) {
    const urls = startUrlsText.value
      .split("\n")
      .map((u) => u.trim())
      .filter((u) => u);
    formData.value.startUrls = urls;
  } else {
    formData.value.startUrls = [];
  }
};

const handleSubmit = async () => {
  try {
    handleStartUrlsBlur(); // ensure sync
    await formRef.value.validate();

    if (!formData.value.startUrls || formData.value.startUrls.length === 0) {
      message.warning("请输入至少一个起始URL");
      return;
    }

    submitting.value = true;
    const submitData = { ...formData.value };
    // 确保数值类型正确
    submitData.maxDepth = Number(submitData.maxDepth);
    submitData.requestInterval = Number(submitData.requestInterval);
    // ...

    if (dialogType.value === "add") {
      await fetchAddTask(submitData);
      message.success("新增成功");
    } else {
      await fetchEditTask(submitData);
      message.success("更新成功");
    }
    dialogVisible.value = false;
    loadData();
  } catch (e) {
    console.error(e);
  } finally {
    submitting.value = false;
  }
};

// 任务控制
const handleStartTask = async (record: any) => {
  try {
    await fetchStartTask(record.id);
    message.success("任务已启动");
    loadData();
  } catch (e: any) {
    message.error(e.message || "启动失败");
  }
};

const handlePauseTask = async (record: any) => {
  try {
    await fetchPauseTask(record.id);
    message.success("任务已暂停");
    loadData();
  } catch (e) {}
};

const handleResumeTask = async (record: any) => {
  try {
    await fetchResumeTask(record.id);
    message.success("任务已恢复");
    loadData();
  } catch (e) {}
};

const handleStopTask = async (record: any) => {
  try {
    await fetchStopTask(record.id);
    message.success("任务已停止");
    loadData();
  } catch (e) {}
};

const handleDelete = async (record: any) => {
  try {
    await fetchDeleteTask(record.id);
    message.success("删除成功");
    loadData();
  } catch (e: any) {
    message.error(e.message || "删除失败");
  }
};

const handleBatchDelete = () => {
  if (!selectedRowKeys.value.length) return;
  Modal.confirm({
    title: "确认删除",
    content: `确定要删除选中的 ${selectedRowKeys.value.length} 个任务吗？`,
    onOk: async () => {
      try {
        // 并行删除? 或者后端支持批量? 这里假设循环调接口
        await Promise.all(
          selectedRowKeys.value.map((id) => fetchDeleteTask(id)),
        );
        message.success("批量删除成功");
        selectedRowKeys.value = [];
        loadData();
      } catch (e) {
        message.error("部分删除失败");
      }
    },
  });
};
</script>

<style scoped>
.page-container {
  padding: 8px;
}
.status-cell {
  display: flex;
  align-items: center;
}
</style>
