<template>
  <div class="page-container">
    <a-card title="部门管理">
      <template #extra>
        <a-button type="primary" @click="handleAdd(null)">
          <template #icon><PlusOutlined /></template>
          新增
        </a-button>
      </template>

      <a-table
        :columns="columns"
        :dataSource="deptList"
        rowKey="id"
        :defaultExpandAllRows="true"
        :loading="loading"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)"
                >编辑</a-button
              >
              <a-button type="link" size="small" @click="handleAdd(record)"
                >新增</a-button
              >
              <a-popconfirm
                title="确定要删除这个部门吗？"
                ok-text="确定"
                cancel-text="取消"
                @confirm="handleDelete(record)"
              >
                <a-button type="link" size="small" danger>删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑部门弹窗 -->
    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirmLoading="modalLoading"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="上级部门" name="parentId">
          <a-tree-select
            v-model:value="formData.parentId"
            :tree-data="deptTreeData"
            :field-names="{
              label: 'deptName',
              value: 'id',
              children: 'children',
            }"
            placeholder="请选择上级部门"
            allow-clear
            tree-default-expand-all
          />
        </a-form-item>
        <a-form-item label="部门名称" name="deptName">
          <a-input
            v-model:value="formData.deptName"
            placeholder="请输入部门名称"
          />
        </a-form-item>
        <a-form-item label="显示排序" name="orderNum">
          <a-input-number
            v-model:value="formData.orderNum"
            :min="0"
            style="width: 100%"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from "vue";
import { PlusOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import type { FormInstance, Rule } from "ant-design-vue/es/form";
import request from "../../../utils/request";

/**
 * 部门项接口定义
 */
interface DeptItem {
  id: number;
  deptName: string;
  parentId: number;
  orderNum: number;
  children?: DeptItem[];
}

/**
 * 表单数据接口定义
 */
interface FormData {
  id?: number;
  deptName: string;
  parentId: number | null;
  orderNum: number;
}

// 表格数据
const deptList = ref<DeptItem[]>([]);
const loading = ref(false);

// 弹窗相关
const modalVisible = ref(false);
const modalLoading = ref(false);
const modalTitle = ref("新增部门");
const formRef = ref<FormInstance>();
const isEdit = ref(false);

// 表单数据
const formData = ref<FormData>({
  deptName: "",
  parentId: null,
  orderNum: 0,
});

// 表单校验规则
const formRules: Record<string, Rule[]> = {
  deptName: [
    { required: true, message: "请输入部门名称", trigger: "blur" },
    { min: 2, max: 50, message: "部门名称长度为 2-50 个字符", trigger: "blur" },
  ],
};

// 表格列定义
const columns = [
  { title: "部门名称", dataIndex: "deptName", key: "deptName" },
  { title: "排序", dataIndex: "orderNum", key: "orderNum", width: 80 },
  { title: "操作", key: "action", width: 200 },
];

/**
 * 部门树数据（用于下拉选择）
 * 非超管用户不显示"顶级部门"选项，只能在现有部门下创建子部门
 */
const deptTreeData = computed(() => {
  return deptList.value;
});

/**
 * 加载部门列表
 */
const loadDepts = async () => {
  loading.value = true;
  try {
    const res = await request.get("/system/dept/list");
    deptList.value = (res as any) || [];
  } catch (error) {
    console.error("加载部门列表失败", error);
    message.error("加载部门列表失败");
  } finally {
    loading.value = false;
  }
};

/**
 * 新增部门
 * @param parent 父级部门，如果为null则需要用户选择
 */
const handleAdd = (parent: DeptItem | null) => {
  isEdit.value = false;

  if (parent) {
    // 行内新增：直接设置父部门
    modalTitle.value = `新增子部门（${parent.deptName}）`;
    formData.value = {
      deptName: "",
      parentId: parent.id,
      orderNum: 0,
    };
  } else {
    // 顶部新增：需要用户选择上级部门
    modalTitle.value = "新增部门";
    // 默认选择第一个部门作为父级（如果存在）
    const firstDept = deptList.value[0];
    formData.value = {
      deptName: "",
      parentId: firstDept?.id ?? null,
      orderNum: 0,
    };
  }

  modalVisible.value = true;
};

/**
 * 编辑部门
 * @param record 部门记录
 */
const handleEdit = (record: DeptItem) => {
  isEdit.value = true;
  modalTitle.value = "编辑部门";
  formData.value = {
    id: record.id,
    deptName: record.deptName,
    parentId: record.parentId,
    orderNum: record.orderNum,
  };
  modalVisible.value = true;
};

/**
 * 删除部门
 * @param record 部门记录
 */
const handleDelete = async (record: DeptItem) => {
  try {
    await request.delete(`/system/dept/${record.id}`);
    message.success("删除成功");
    loadDepts();
  } catch (error: any) {
    console.error("删除部门失败", error);
    message.error(error?.message || "删除失败");
  }
};

/**
 * 弹窗确认
 */
const handleModalOk = async () => {
  try {
    await formRef.value?.validate();
    modalLoading.value = true;

    if (isEdit.value && formData.value.id) {
      // 编辑
      await request.put(`/system/dept/${formData.value.id}`, {
        deptName: formData.value.deptName,
        parentId: formData.value.parentId,
        orderNum: formData.value.orderNum,
      });
      message.success("修改成功");
    } else {
      // 新增
      await request.post("/system/dept", {
        deptName: formData.value.deptName,
        parentId: formData.value.parentId,
        orderNum: formData.value.orderNum,
      });
      message.success("新增成功");
    }

    modalVisible.value = false;
    loadDepts();
  } catch (error: any) {
    if (error?.errorFields) {
      // 表单校验失败
      return;
    }
    console.error("保存部门失败", error);
    message.error(error?.message || "保存失败");
  } finally {
    modalLoading.value = false;
  }
};

/**
 * 弹窗取消
 */
const handleModalCancel = () => {
  modalVisible.value = false;
  formRef.value?.resetFields();
};

onMounted(() => {
  loadDepts();
});
</script>

<style scoped>
.page-container {
  padding: 8px;
}
</style>
