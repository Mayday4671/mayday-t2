<template>
  <div class="image-page">
    <!-- 顶部工具栏 -->
    <a-card :bordered="false" class="header-card">
      <div class="page-header">
        <div class="header-left">
          <h2 class="page-title">
            <PictureOutlined />
            图片列表
          </h2>
          <a-tag color="blue">共 {{ pagination.total }} 篇文章</a-tag>
        </div>
        <div class="header-right">
          <a-input-search
            v-model:value="queryParam.title"
            placeholder="搜索文章标题..."
            allow-clear
            style="width: 280px"
            @search="handleSearch"
          />
          <a-button @click="fetchList"> <ReloadOutlined /> 刷新 </a-button>
        </div>
      </div>
    </a-card>

    <!-- 文章封面网格 -->
    <a-spin :spinning="loading">
      <div class="article-grid">
        <div
          v-for="item in dataList"
          :key="item.articleId"
          class="article-card"
          @click="openArticleDetail(item)"
        >
          <!-- 封面图片 -->
          <div class="card-cover">
            <img
              :src="item.coverUrl || fallbackImage"
              :alt="item.articleTitle"
              @error="(e: Event) => handleImageError(e)"
            />
            <!-- 渐变遮罩 -->
            <div class="cover-overlay"></div>
            <!-- 图片数量 -->
            <div class="image-count">
              <PictureOutlined /> {{ item.imageCount }}张
            </div>
            <!-- 删除按钮 -->
            <a-popconfirm
              title="确定删除该文章下的所有图片吗？"
              @confirm="handleDeleteArticleImages(item)"
              placement="topRight"
            >
              <div class="delete-btn" @click.stop>
                <DeleteOutlined />
              </div>
            </a-popconfirm>
          </div>
          <!-- 卡片信息 -->
          <div class="card-body">
            <div class="article-title" :title="item.articleTitle">
              {{ item.articleTitle || "无标题文章" }}
            </div>
            <div class="article-info">
              <span class="source">{{ item.sourceSite || "未知来源" }}</span>
              <span class="date">{{ formatDate(item.publishTime) }}</span>
            </div>
          </div>
        </div>
      </div>
    </a-spin>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="pagination.total > 0">
      <a-pagination
        v-model:current="pagination.current"
        v-model:pageSize="pagination.pageSize"
        :total="pagination.total"
        :showSizeChanger="true"
        :showQuickJumper="true"
        :showTotal="(total: number) => `共 ${total} 条`"
        @change="handlePageChange"
      />
    </div>

    <!-- 空状态 -->
    <a-empty
      v-if="!loading && dataList.length === 0"
      description="暂无图片数据"
      style="margin-top: 60px"
    />

    <!-- 图片详情抽屉 -->
    <a-drawer
      v-model:open="drawerVisible"
      :width="800"
      placement="right"
      :title="null"
      :headerStyle="{ padding: '16px 24px', borderBottom: '1px solid #f0f0f0' }"
      :bodyStyle="{ padding: '0', background: '#fafafa' }"
    >
      <template #title>
        <div class="drawer-header">
          <div class="drawer-title-text">
            {{ currentArticle?.articleTitle || "图片详情" }}
          </div>
          <a-tag color="processing">{{ detailList.length }} 张图片</a-tag>
        </div>
      </template>
      <template #extra>
        <a-popconfirm
          title="确定清空该文章下的所有图片吗？"
          @confirm="handleDeleteArticleImages(currentArticle)"
        >
          <a-button type="primary" danger size="small">
            <template #icon><DeleteOutlined /></template>
            清空图片
          </a-button>
        </a-popconfirm>
      </template>

      <div class="drawer-content">
        <a-spin :spinning="detailLoading">
          <a-empty
            v-if="detailList.length === 0 && !detailLoading"
            description="暂无图片"
            style="padding: 60px 0"
          />

          <!-- 图片网格 - 使用 PreviewGroup 支持左右切换 -->
          <a-image-preview-group v-if="detailList.length > 0">
            <div class="image-grid">
              <div
                v-for="(img, index) in detailList"
                :key="img.id"
                class="image-item"
              >
                <div class="image-thumb">
                  <a-image
                    :src="getImageUrl(img)"
                    :alt="img.fileName"
                    :fallback="fallbackImage"
                    :preview="{ src: getImageUrl(img) }"
                    class="cover-image"
                  />
                  <div class="image-index">
                    {{ index + 1 }}/{{ detailList.length }}
                  </div>
                </div>
                <div class="image-meta">
                  <div class="file-name" :title="img.fileName">
                    {{ img.fileName }}
                  </div>
                  <div class="file-size">{{ formatSize(img.fileSize) }}</div>
                </div>
              </div>
            </div>
          </a-image-preview-group>
        </a-spin>
      </div>
    </a-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from "vue";
import { message } from "ant-design-vue";
import {
  PictureOutlined,
  DeleteOutlined,
  ReloadOutlined,
} from "@ant-design/icons-vue";
import {
  fetchGetImageArticleCoverList,
  fetchGetImagesByArticle,
  fetchDeleteImagesByArticle,
} from "../../../../api/admin/crawler";
import dayjs from "dayjs";

defineOptions({ name: "ImagePage" });

const fallbackImage =
  "data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMzAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMTAwJSIgaGVpZ2h0PSIxMDAlIiBfiWxsPSIjZjVmNWY1Ii8+PHRleHQgeD0iNTAlIiB5PSI1MCUiIGZpbGw9IiNiZGJkYmQiIGZvbnQtc2l6ZT0iMTQiIHRleHQtYW5jaG9yPSJtaWRkbGUiIGR5PSIuM2VtIj5ObyBJbWFnZTwvdGV4dD48L3N2Zz4=";

const loading = ref(false);
const dataList = ref<any[]>([]);
const queryParam = reactive({ title: "" });

const pagination = reactive({
  current: 1,
  pageSize: 15,
  total: 0,
});

// 详情相关
const drawerVisible = ref(false);
const currentArticle = ref<any>(null);
const detailList = ref<any[]>([]);
const detailLoading = ref(false);

// 固定分页大小，不再自适应计算

const handleSearch = () => {
  pagination.current = 1;
  fetchList();
};

const handlePageChange = (page: number, pageSize: number) => {
  pagination.current = page;
  pagination.pageSize = pageSize;
  fetchList();
};

const fetchList = async () => {
  loading.value = true;
  console.log(
    "[DEBUG] fetchList called, pageSize:",
    pagination.pageSize,
    "current:",
    pagination.current,
  );
  try {
    const res: any = await fetchGetImageArticleCoverList({
      current: pagination.current,
      pageSize: pagination.pageSize,
      title: queryParam.title,
    });
    console.log("[DEBUG] API response:", res);
    console.log(
      "[DEBUG] records count:",
      res.records?.length,
      "totalRow:",
      res.totalRow || res.total,
    );
    dataList.value = res.records || [];
    pagination.total = res.totalRow || res.total || 0;
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const openArticleDetail = async (article: any) => {
  currentArticle.value = article;
  drawerVisible.value = true;
  detailLoading.value = true;
  detailList.value = [];

  try {
    const res: any = await fetchGetImagesByArticle(article.articleId);
    console.log("图片列表响应:", res); // 调试日志
    detailList.value = res || [];
  } catch (e) {
    message.error("加载图片详情失败");
    console.error(e);
  } finally {
    detailLoading.value = false;
  }
};

const handleDeleteArticleImages = async (article: any) => {
  if (!article) return;
  try {
    await fetchDeleteImagesByArticle(article.articleId);
    message.success("删除成功");
    if (drawerVisible.value) {
      drawerVisible.value = false;
    }
    fetchList();
  } catch (e) {
    message.error("删除失败");
  }
};

// 获取图片URL，优先使用displayUrl，回退到url
const getImageUrl = (img: any) => {
  // 优先使用 displayUrl（本地已下载的图片）
  if (img.displayUrl) {
    return img.displayUrl;
  }
  // 回退到原始 URL
  if (img.url) {
    return img.url;
  }
  return fallbackImage;
};

const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement;
  target.src = fallbackImage;
};

const formatDate = (dateStr: string) => {
  if (!dateStr) return "-";
  return dayjs(dateStr).format("YYYY-MM-DD");
};

const formatSize = (bytes: number) => {
  if (!bytes || bytes === 0) return "0 B";
  const k = 1024;
  const sizes = ["B", "KB", "MB", "GB"];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + " " + sizes[i];
};

onMounted(() => {
  fetchList();
});
</script>

<style scoped>
.image-page {
  padding: 16px;
  background: #f0f2f5;
  /* min-height removed to avoid scrollbar issues */
}

/* 顶部卡片 */
.header-card {
  margin-bottom: 16px;
  border-radius: 8px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 8px;
  color: #1f2937;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 文章网格 */
.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}

/* 文章卡片 */
.article-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid #e5e7eb;
}

.article-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
  border-color: #1890ff;
}

.card-cover {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: #f5f5f5;
}

.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.4s ease;
}

.article-card:hover .card-cover img {
  transform: scale(1.05);
}

.cover-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 50%;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.4));
  pointer-events: none;
}

.image-count {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background: rgba(24, 144, 255, 0.9);
  color: #fff;
  padding: 4px 10px;
  border-radius: 16px;
  font-size: 12px;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 4px;
}

.delete-btn {
  position: absolute;
  top: 10px;
  right: 10px;
  width: 32px;
  height: 32px;
  background: rgba(255, 77, 79, 0.9);
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transform: scale(0.8);
  transition: all 0.2s ease;
  cursor: pointer;
}

.article-card:hover .delete-btn {
  opacity: 1;
  transform: scale(1);
}

.delete-btn:hover {
  background: rgba(255, 77, 79, 1);
}

.card-body {
  padding: 14px;
}

.article-title {
  color: #1f2937;
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 8px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  line-height: 1.4;
}

.article-info {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #9ca3af;
}

/* 分页 */
.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
}

/* 抽屉头部 */
.drawer-header {
  display: flex;
  align-items: center;
  gap: 12px;
}

.drawer-title-text {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  max-width: 400px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.drawer-content {
  padding: 20px;
  min-height: 400px;
}

/* 图片网格 */
.image-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.image-item {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  transition: all 0.2s ease;
  cursor: pointer;
}

.image-item:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  transform: translateY(-2px);
}

.image-thumb {
  position: relative;
  height: 140px;
  background: #f5f5f5;
  overflow: hidden;
}

.image-thumb :deep(.ant-image),
.image-thumb :deep(.ant-image-img) {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.image-thumb :deep(.ant-image-img) {
  transition: transform 0.3s ease;
}

.image-item:hover :deep(.ant-image-img) {
  transform: scale(1.05);
}

.image-hover {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  opacity: 0;
  transition: opacity 0.2s ease;
}

.image-item:hover .image-hover {
  opacity: 1;
}

.image-meta {
  padding: 10px 12px;
}

.file-name {
  font-size: 12px;
  color: #374151;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-bottom: 4px;
}

.file-size {
  font-size: 11px;
  color: #9ca3af;
}
</style>
