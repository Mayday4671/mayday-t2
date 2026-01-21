<template>
  <div class="portal-container">
    <!-- Header -->
    <header class="portal-header">
      <div class="header-content">
        <div class="logo-area" @click="$router.push('/')" style="cursor: pointer">
          <span class="logo-text">Mayday</span>
          <span class="logo-sub">Insight</span>
        </div>

        <nav class="main-nav">
          <a class="nav-item" @click="$router.push('/')">首页</a>
          <a class="nav-item">沸点</a>
          <a class="nav-item">课程</a>
          <a class="nav-item">直播</a>
        </nav>

        <div class="action-area">
          <a-button type="text" @click="$router.push('/')">
            <arrow-left-outlined/>
            返回列表
          </a-button>
        </div>
      </div>
    </header>

    <!-- Main Layout -->
    <div class="portal-layout">

      <!-- Center Content (Article Detail) -->
      <main class="main-feed">
        <div class="article-wrapper" v-if="article">
          <div class="article-flex-container">
            <!-- Left Column: Article Content -->
            <div class="article-main-col">
              <!-- Hero Section -->
              <div class="article-hero">
                <div class="hero-meta">
                  <a-tag color="blue" v-if="article.sourceSite">{{ article.sourceSite }}</a-tag>
                  <span class="publish-time">{{ formatDate(article.publishTime) }}</span>
                </div>
                <h1 class="article-title">{{ article.title }}</h1>

                <div class="author-card">
                  <div class="avatar" :style="{ background: getRandomColor(article.id) }">{{
                      (article.author || 'M')[0]
                    }}
                  </div>
                  <div class="author-info">
                    <div class="author-name">{{ article.author || 'Mayday Author' }}</div>
                    <div class="author-desc">发布于 {{ article.sourceSite || '全网' }} · 阅读
                      {{ Math.floor(Math.random() * 5000) + 500 }}
                    </div>
                  </div>
                  <a-button type="primary" shape="round" size="small" class="follow-btn">
                    <plus-outlined/>
                    关注
                  </a-button>
                </div>
              </div>

              <!-- Cover Image (Featured) -->
              <div class="featured-image" v-if="article.coverImage">
                <img :src="article.coverImage" alt="Cover"/>
              </div>

              <!-- Content Body -->
              <div class="article-content" id="detail-content">
                <div class="detail-body typo" v-html="renderedContent"></div>
              </div>

              <!-- Image Gallery -->
              <div class="gallery-section" id="detail-gallery" v-if="article.imageList && article.imageList.length > 0">
                <div class="section-divider">
                  <span><picture-outlined/> 图片画廊</span>
                </div>
                <div class="gallery-grid">
                  <div class="gallery-item" v-for="(img, index) in article.imageList" :key="index"
                       @click="previewImage(img)">
                    <img :src="img" loading="lazy"/>
                    <div class="gallery-overlay">
                      <eye-outlined/>
                    </div>
                  </div>
                </div>
              </div>

              <!-- Article Footer -->
              <div class="article-footer">
                <div class="original-link" v-if="article.sourceSite">
                  <span>文章来源：</span>
                  <a v-if="article.url" :href="article.url" target="_blank"
                     rel="noopener noreferrer">{{ article.sourceSite }}
                    <export-outlined/>
                  </a>
                  <span v-else>{{ article.sourceSite }}</span>
                </div>
                <div class="interaction-bar">
                  <a-button shape="circle" size="large">
                    <like-outlined/>
                  </a-button>
                  <a-button shape="circle" size="large">
                    <star-outlined/>
                  </a-button>
                  <a-button shape="circle" size="large">
                    <share-alt-outlined/>
                  </a-button>
                </div>
              </div>
            </div>

            <!-- Right Column: Internal TOC -->
            <div class="article-toc-col" v-if="tocList.length > 0">
              <div class="internal-toc-widget">
                <div class="toc-header">目录</div>
                <div class="toc-list-inner">
                  <div
                      class="toc-item-inner"
                      v-for="(item, index) in tocList"
                      :key="index"
                      :class="{ active: activeHeadingId === item.id }"
                      :style="{ paddingLeft: (item.level - 1) * 12 + 'px' }"
                      @click="scrollToSection(item.id)"
                  >
                    {{ item.text }}
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div> <!-- End of article-wrapper -->

        <!-- recommendations -->
        <div class="recommend-section" v-if="recommendList.length > 0">
          <div class="section-title">推荐阅读</div>
          <div class="recommend-grid">
            <div class="recommend-card widget-card" v-for="item in recommendList.slice(0, 3)" :key="item.id"
                 @click="goToDetail(item.id)">
              <div class="rc-cover" v-if="item.coverImage">
                <img :src="item.coverImage" loading="lazy"/>
              </div>
              <div class="rc-info">
                <div class="rc-title">{{ item.title }}</div>
                <div class="rc-meta">{{ formatDate(item.publishTime) }}</div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <aside class="sidebar-right">
        <div class="sidebar-scroll-content">
          <div class="widget-card author-widget">
            <div class="widget-header">关于作者</div>
            <div class="widget-body">
              <div class="author-row">
                <div class="avatar-lg" :style="{ background: article ? getRandomColor(article.id) : '#ccc' }">
                  {{ article ? (article.author || 'M')[0] : 'M' }}
                </div>
                <div class="info">
                  <div class="name">{{ article ? article.author : 'Loading' }}</div>
                  <div class="role">内容创作者</div>
                </div>
              </div>
            </div>
          </div>
          <div class="widget-card recommend-widget" v-if="recommendList.length > 0">
            <div class="widget-header">热门推荐</div>
            <div class="side-list">
              <div class="side-item" v-for="item in recommendList.slice(0, 5)" :key="item.id"
                   @click="goToDetail(item.id)">
                <div class="side-thumb" v-if="item.coverImage">
                  <img :src="item.coverImage"/>
                </div>
                <div class="side-content">
                  <div class="side-title">{{ item.title }}</div>
                  <div class="side-date">{{ formatDate(item.publishTime) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, watch, computed, onUnmounted} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {
  ArrowLeftOutlined, PlusOutlined, LikeOutlined, StarOutlined,
  ShareAltOutlined, ExportOutlined, EyeOutlined, PictureOutlined
} from '@ant-design/icons-vue';
import {fetchPortalArticleDetail, fetchPortalArticleList} from '../../../api/frontend/portal';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import {Empty} from 'ant-design-vue';
import {marked} from "marked";
import hljs from "highlight.js";
import "highlight.js/styles/atom-one-dark.css";

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const route = useRoute();
const router = useRouter();
const loading = ref(false);
const article = ref<any>(null);
const recommendList = ref<any[]>([]);
const tocList = ref<any[]>([]);
const activeHeadingId = ref('');

const getRandomColor = (id: number = 0) => {
  const colors = ['#FF9AA2', '#FFB7B2', '#FFDAC1', '#E2F0CB', '#B5EAD7', '#C7CEEA'];
  return colors[id % colors.length];
};

const renderer = new marked.Renderer();
renderer.code = ({text, lang}: any) => {
  const language = hljs.getLanguage(lang) ? lang : 'plaintext';
  const highlighted = hljs.highlight(text, {language}).value;
  return `<div class="code-block-wrapper">
            <div class="code-block-header"><span class="code-lang">${lang || 'text'}</span></div>
            <pre><code class="hljs language-${language}">${highlighted}</code></pre>
          </div>`;
};

renderer.heading = ({text, depth}: any) => {
  const id = `heading-${tocList.value.length}`;
  const plainText = text.replace(/<[^>]+>/g, '');
  tocList.value.push({id, text: plainText, level: depth});
  return `<h${depth} id="${id}">${text}</h${depth}>`;
};

marked.use({renderer, gfm: true, breaks: true});

const renderedContent = computed(() => {
  if (!article.value) return '';
  tocList.value = [];
  return marked.parse(article.value.content || article.value.summary || '暂无内容') as string;
});

const formatDate = (dateStr?: string) => dateStr ? dayjs(dateStr).format('YYYY年MM月DD日 HH:mm') : '刚刚';

const loadDetail = async (id: number) => {
  loading.value = true;
  try {
    article.value = await fetchPortalArticleDetail(id);
  } catch (e) {
    console.error(e);
  } finally {
    loading.value = false;
  }
};

const loadRecommendations = async () => {
  try {
    const res: any = await fetchPortalArticleList({current: 1, pageSize: 6, title: ''});
    recommendList.value = res.records || [];
  } catch (e) {
    console.error(e);
  }
};

const previewImage = (src: string) => {
  window.open(src, '_blank');
};

const isScrollingByClick = ref(false);
let scrollTimeout: any = null;

const scrollToSection = (id: string) => {
  activeHeadingId.value = id;
  isScrollingByClick.value = true;
  const el = document.getElementById(id);
  const container = document.querySelector('.portal-layout');
  if (el && container) {
    const rect = el.getBoundingClientRect();
    const containerRect = container.getBoundingClientRect();
    const targetScrollTop = container.scrollTop + rect.top - containerRect.top - 24;
    container.scrollTo({top: targetScrollTop, behavior: 'smooth'});
  }
  if (scrollTimeout) clearTimeout(scrollTimeout);
  scrollTimeout = setTimeout(() => {
    isScrollingByClick.value = false;
  }, 800);
};

const handleScroll = () => {
  if (isScrollingByClick.value) return;
  const headings = tocList.value;
  const container = document.querySelector('.portal-layout');
  if (!headings.length || !container) return;

  let currentId = '';
  for (const heading of headings) {
    const el = document.getElementById(heading.id);
    if (el) {
      if (el.getBoundingClientRect().top - container.getBoundingClientRect().top <= 30) {
        currentId = heading.id;
      } else break;
    }
  }
  activeHeadingId.value = currentId || headings[0]?.id || '';
};

onMounted(() => {
  if (route.params.id) {
    loadDetail(Number(route.params.id));
    loadRecommendations();
  }
  document.querySelector('.portal-layout')?.addEventListener('scroll', handleScroll);
});

onUnmounted(() => {
  document.querySelector('.portal-layout')?.removeEventListener('scroll', handleScroll);
  if (scrollTimeout) clearTimeout(scrollTimeout);
});

const goToDetail = (id: number) => {
  router.push(`/article/${id}`);
};

watch(() => route.params.id, (newId) => {
  if (newId) {
    loadDetail(Number(newId));
    document.querySelector('.portal-layout')?.scrollTo(0, 0);
  }
});
</script>

<style scoped>
.portal-container {
  height: 100vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  background: #f7f8fa;
}

.portal-header {
  height: 64px;
  background: #fff;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  position: sticky;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
}

.header-content {
  width: 100%;
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-area {
  font-size: 1.5rem;
  font-weight: 800;
  color: #1677ff;
  letter-spacing: -0.5px;
}

.logo-sub {
  color: #1d1d1f;
  margin-left: 4px;
  font-weight: 400;
}

.main-nav {
  display: flex;
  gap: 32px;
}

.nav-item {
  color: #515767;
  font-size: 1rem;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
}

.nav-item:hover {
  color: #1677ff;
}

.portal-layout {
  flex: 1;
  display: flex;
  gap: 20px;
  padding: 20px;
  overflow-y: auto;
  height: calc(100vh - 64px);
  //max-width: 1400px;
  margin: 0 auto;
  width: 100%;
}

.main-feed {
  flex: 1;
  min-width: 0;
}

.sidebar-right {
  width: 300px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  height: fit-content;
}

.article-wrapper {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.author-card {
  display: flex;
  align-items: center;
  margin-bottom: 24px;
}

.avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: bold;
  margin-right: 12px;
}

.author-info {
  flex: 1;
}

.author-name {
  font-weight: 600;
  color: #333;
}

.author-desc {
  font-size: 12px;
  color: #86909c;
}

.article-flex-container {
  display: flex;
  gap: 30px;
}

.article-main-col {
  flex: 1;
  min-width: 0;
}

.article-toc-col {
  width: 220px;
  position: sticky;
  top: 0;
  border-left: 1px solid #f0f0f0;
  padding-left: 15px;
}

.toc-header {
  font-weight: bold;
  margin-bottom: 15px;
  color: #1677ff;
}

.toc-list-inner {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.toc-item-inner {
  padding: 6px;
  cursor: pointer;
  font-size: 14px;
  border-left: 2px solid transparent;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  color: #666;
  transition: all 0.3s;
}

.toc-item-inner:hover {
  color: #1677ff;
}

.toc-item-inner.active {
  color: #1677ff;
  border-left-color: #1677ff;
  background: #e6f7ff;
  font-weight: 600;
}

/* Markdown Typography */
.detail-body {
  font-size: 16px;
  line-height: 1.8;
  color: #333;
}

.detail-body :deep(h1), .detail-body :deep(h2), .detail-body :deep(h3) {
  margin-top: 30px;
  margin-bottom: 15px;
  font-weight: bold;
}

.detail-body :deep(p) {
  margin-bottom: 16px;
}

.detail-body :deep(img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 20px 0;
}

/* Markdown Table - Typora Style */
.detail-body :deep(table) {
  width: 100% !important;
  border-collapse: collapse !important;
  margin: 20px 0 !important;
  border: 1px solid #dfe2e5 !important;
}

.detail-body :deep(th), .detail-body :deep(td) {
  border: 1px solid #dfe2e5 !important;
  padding: 8px 12px !important;
  text-align: left;
}

.detail-body :deep(th) {
  background-color: #f6f8fa !important;
  font-weight: 600;
}

.detail-body :deep(tr:nth-child(2n)) {
  background-color: #fafbfc;
}

/* Code Blocks */
.detail-body :deep(.code-block-wrapper) {
  margin: 15px 0;
  background: #282c34;
  border-radius: 6px;
  overflow: hidden;
}

.detail-body :deep(.code-block-header) {
  background: #21252b;
  padding: 4px 12px;
  text-align: right;
  color: #abb2bf;
  font-size: 12px;
  border-bottom: 1px solid #3e4451;
}

.detail-body :deep(pre) {
  margin: 0;
  padding: 15px;
  overflow-x: auto;
}

.detail-body :deep(code.hljs) {
  background: transparent;
  padding: 0;
  border: none;
}

.recommend-section {
  margin-top: 30px;
  padding: 0 10px;
}

.section-title {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
}

.section-title::before {
  content: '';
  width: 4px;
  height: 18px;
  background: #1677ff;
  margin-right: 8px;
  border-radius: 2px;
}

.recommend-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.widget-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: transform 0.3s;
}

.widget-card:hover {
  transform: translateY(-4px);
}

.rc-cover {
  height: 140px;
}

.rc-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.rc-info {
  padding: 12px;
}

.rc-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 8px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.rc-meta {
  font-size: 12px;
  color: #999;
}

.widget-card.author-widget {
  cursor: default;
  padding: 16px;
}

.widget-card.author-widget:hover {
  transform: none;
}

.widget-header {
  font-weight: bold;
  margin-bottom: 12px;
  font-size: 16px;
}

.side-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
  cursor: pointer;
}

.side-thumb {
  width: 60px;
  height: 45px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.side-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.side-content {
  flex: 1;
  min-width: 0;
}

.side-title {
  font-size: 13px;
  font-weight: 600;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
</style>
