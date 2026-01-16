<template>
  <div class="page-container">
    <a-card title="代理配置管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="showDialog('add')">
          <template #icon><PlusOutlined /></template>
          新增代理
        </a-button>
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
          <template v-if="column.key === 'enabled'">
            <a-tag :color="record.enabled ? 'green' : 'red'">
              {{ record.enabled ? "启用" : "禁用" }}
            </a-tag>
          </template>

          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="showDialog('edit', record)"
                >编辑</a-button
              >
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record)">
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <a-modal
      v-model:open="dialogVisible"
      :title="dialogType === 'add' ? '新增代理' : '编辑代理'"
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
        <a-form-item label="代理名称" name="proxyName">
          <a-input
            v-model:value="formData.proxyName"
            placeholder="例如: 美国代理01"
          />
        </a-form-item>

        <a-form-item label="代理类型" name="proxyType">
          <a-radio-group v-model:value="formData.proxyType">
            <a-radio value="SOCKS">SOCKS</a-radio>
            <a-radio value="HTTP">HTTP</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="主机地址" name="host">
          <a-input
            v-model:value="formData.host"
            placeholder="例如: 127.0.0.1"
          />
        </a-form-item>

        <a-form-item label="端口" name="port">
          <a-input-number
            v-model:value="formData.port"
            :min="1"
            :max="65535"
            style="width: 100%"
          />
        </a-form-item>

        <a-form-item label="用户名" name="username">
          <a-input v-model:value="formData.username" placeholder="选填" />
        </a-form-item>

        <a-form-item label="密码" name="password">
          <a-input-password
            v-model:value="formData.password"
            placeholder="选填"
          />
        </a-form-item>

        <a-form-item label="启用状态" name="enabled">
          <a-switch
            v-model:checked="formData.enabled"
            :checked-value="1"
            :un-checked-value="0"
          />
        </a-form-item>

        <a-form-item label="排序" name="sort">
          <a-input-number
            v-model:value="formData.sort"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from "vue";
import { message } from "ant-design-vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import type { TablePaginationConfig } from "ant-design-vue";
import {
  fetchGetProxyList,
  fetchAddProxy,
  fetchEditProxy,
  fetchDeleteProxy,
} from "../../../../api/admin/crawler";

defineOptions({ name: "ProxyPage" });

const loading = ref(false);
const dataList = ref<any[]>([]);
const submitting = ref(false);

const pagination = reactive<TablePaginationConfig>({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
});

const columns = [
  { title: "ID", dataIndex: "id", width: 60 },
  { title: "名称", dataIndex: "proxyName", width: 150 },
  { title: "类型", dataIndex: "proxyType", width: 80 },
  {
    title: "地址",
    customRender: ({ record }: any) => `${record.host}:${record.port}`,
    width: 150,
  },
  { title: "用户名", dataIndex: "username", width: 100 },
  { title: "状态", key: "enabled", width: 80 },
  { title: "排序", dataIndex: "sort", width: 60 },
  { title: "更新时间", dataIndex: "updateTime", width: 170 },
  { title: "操作", key: "action", width: 150, fixed: "right" },
];

// 表单
const dialogVisible = ref(false);
const dialogType = ref<"add" | "edit">("add");
const formRef = ref();
const formData = ref<any>({});

const formRules = {
  proxyName: [{ required: true, message: "请输入名称", trigger: "blur" }],
  proxyType: [{ required: true, message: "请选择类型", trigger: "change" }],
  host: [{ required: true, message: "请输入主机地址", trigger: "blur" }],
  port: [{ required: true, message: "请输入端口", trigger: "blur" }],
};

onMounted(() => {
  fetchList();
});

const fetchList = async () => {
  loading.value = true;
  try {
    const res: any = await fetchGetProxyList({
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

const showDialog = (type: "add" | "edit", record?: any) => {
  dialogType.value = type;
  if (type === "edit" && record) {
    formData.value = { ...record };
  } else {
    formData.value = {
      proxyType: "SOCKS",
      enabled: 1,
      sort: 0,
    };
  }
  dialogVisible.value = true;
  nextTick(() => {
    formRef.value?.clearValidate();
  });
};

const handleSubmit = async () => {
  try {
    await formRef.value.validate();
    submitting.value = true;

    if (dialogType.value === "add") {
      await fetchAddProxy(formData.value);
      message.success("新增成功");
    } else {
      await fetchEditProxy(formData.value);
      message.success("修改成功");
    }
    dialogVisible.value = false;
    fetchList();
  } catch (e) {
    console.error(e);
  } finally {
    submitting.value = false;
  }
};

const handleDelete = async (record: any) => {
  try {
    await fetchDeleteProxy(record.id);
    message.success("删除成功");
    fetchList();
  } catch (e: any) {
    message.error("删除失败");
  }
};
</script>

<style scoped>
.page-container {
  padding: 8px;
}
</style>
