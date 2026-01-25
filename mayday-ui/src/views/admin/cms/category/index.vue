<template>
  <div class="page-container">
    <a-card title="文章分类管理" :bordered="false">
      <template #extra>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增分类
        </a-button>
      </template>

      <a-table
        :loading="loading"
        :dataSource="dataList"
        :columns="columns"
        rowKey="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'icon'">
            <component :is="getIcon(record.icon)" v-if="record.icon" />
            <span v-else>-</span>
          </template>
           <template v-if="column.key === 'code'">
             <a-tag color="blue">{{ record.code }}</a-tag>
          </template>
          <template v-if="column.key === 'status'">
             <a-tag :color="record.status === 1 ? 'green' : 'red'">
                {{ record.status === 1 ? '启用' : '停用' }}
             </a-tag>
          </template>
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">
                <template #icon><EditOutlined /></template>
                编辑
              </a-button>
              <a-popconfirm title="确定删除？" @confirm="handleDelete(record.id)">
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
      v-model:open="visible"
      :title="isEdit ? '编辑分类' : '新增分类'"
      @ok="handleSubmit"
      :confirmLoading="submitting"
    >
      <a-form :model="form" layout="vertical">
        <a-form-item label="分类名称" required>
          <a-input v-model:value="form.name" />
        </a-form-item>
        <a-form-item label="分类编码 (唯一)" required>
          <a-input v-model:value="form.code" placeholder="例如: java" />
        </a-form-item>
        <a-form-item label="图标">
           <IconPicker v-model:value="form.icon" />
        </a-form-item>
        <a-form-item label="排序">
           <a-input-number v-model:value="form.sort" style="width: 100%" />
        </a-form-item>
        <a-form-item label="状态">
           <a-radio-group v-model:value="form.status">
               <a-radio :value="1">启用</a-radio>
               <a-radio :value="0">停用</a-radio>
           </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from "vue";
import { PlusOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons-vue";
import * as Icons from "@ant-design/icons-vue";
import IconPicker from "../../../../components/common/IconPicker.vue";
import { listCmsCategory, addCmsCategory, updateCmsCategory, deleteCmsCategory } from "../../../../api/admin/cms";
import { message } from "ant-design-vue";

const loading = ref(false);
const dataList = ref([]);
const visible = ref(false);
const submitting = ref(false);
const isEdit = ref(false);

const form = reactive({
    id: undefined,
    name: "",
    code: "",
    icon: "",
    sort: 0,
    status: 1
});

const columns = [
    { title: "ID", dataIndex: "id", width: 60 },
    { title: "名称", dataIndex: "name", width: 150 },
    { title: "编码", key: "code", width: 100 },
    { title: "图标", key: "icon", width: 80 },
    { title: "排序", dataIndex: "sort", width: 80 },
    { title: "状态", key: "status", width: 80 },
    { title: "操作", key: "action", width: 150 }
];

const loadData = async () => {
    loading.value = true;
    try {
        const res = await listCmsCategory();
        dataList.value = res || [];
    } catch(e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
}

const handleAdd = () => {
    isEdit.value = false;
    Object.assign(form, { id: undefined, name: "", code: "", icon: "", sort: 0, status: 1 });
    visible.value = true;
}

const handleEdit = (record: any) => {
    isEdit.value = true;
    Object.assign(form, record);
    visible.value = true;
}

const handleDelete = async (id: number) => {
    try {
        await deleteCmsCategory(id);
        message.success("删除成功");
        loadData();
    } catch(e: any) {
        message.error("删除失败");
    }
}

const handleSubmit = async () => {
    if(!form.name || !form.code) {
        message.warning("请完善信息");
        return;
    }
    submitting.value = true;
    try {
        if(isEdit.value) {
            await updateCmsCategory(form);
        } else {
            await addCmsCategory(form);
        }
        visible.value = false;
        message.success("保存成功");
        loadData();
    } catch(e: any) {
        message.error("保存失败");
    } finally {
        submitting.value = false;
    }
}

const getIcon = (name: string) => {
    return (Icons as any)[name];
}

onMounted(() => {
    loadData();
});
</script>

<style scoped>
.page-container { padding: 8px; }
</style>
