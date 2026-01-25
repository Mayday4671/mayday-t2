<template>
  <a-popover
    v-model:open="visible"
    trigger="click"
    placement="bottomLeft"
    overlayClassName="icon-picker-popover"
  >
    <template #content>
      <div class="icon-picker-container" style="width: 300px; max-height: 300px;">
        <a-input-search
          v-model:value="searchText"
          placeholder="搜索图标"
          size="small"
          allow-clear
          style="margin-bottom: 8px"
        />
        <div class="icon-grid" style="display: grid; grid-template-columns: repeat(5, 1fr); gap: 8px; max-height: 260px; overflow-y: auto;">
          <div
            v-for="item in filteredIcons"
            :key="item"
            class="icon-item"
            :class="{ active: value === item }"
            style="text-align: center; cursor: pointer; padding: 4px; border-radius: 4px;"
            @click="handleSelect(item)"
          >
            <component :is="(Icons as any)[item]" style="font-size: 20px" />
          </div>
        </div>
      </div>
    </template>
    
    <a-input
      :value="value"
      placeholder="点击选择图标"
      readonly
      @click="visible = true"
    >
      <template #prefix>
        <component :is="(Icons as any)[value]" v-if="value && (Icons as any)[value]" />
        <component :is="Icons.AppstoreOutlined" v-else />
      </template>
    </a-input>
  </a-popover>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import * as Icons from '@ant-design/icons-vue';

const props = defineProps<{
  value?: string;
}>();

const emit = defineEmits(['update:value', 'change']);

const visible = ref(false);
const searchText = ref('');

// Get all outlined icons
const allIcons = Object.keys(Icons).filter(key => key.endsWith('Outlined'));

const filteredIcons = computed(() => {
  if (!searchText.value) return allIcons.slice(0, 100);
  return allIcons.filter(key => key.toLowerCase().includes(searchText.value.toLowerCase())).slice(0, 100);
});

const handleSelect = (icon: string) => {
  emit('update:value', icon);
  emit('change', icon);
  visible.value = false;
};
</script>

<style scoped>
.icon-item:hover, .icon-item.active {
  background-color: #e6f7ff;
  color: #1890ff;
}
</style>
