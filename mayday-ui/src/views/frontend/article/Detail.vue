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
           <!-- 预留右侧操作区，如搜索、登录等 -->
        </div>
      </div>
    </header>

    <!-- Main Layout -->
    <div class="portal-layout" id="portal-scroll-container" ref="scrollRef">

      <!-- Center Content (Article Detail) -->
      <main class="main-feed">
        <div class="scroll-sentinel" ref="sentinelRef"></div>
        <div class="breadcrumb-bar">
          <a-breadcrumb>
            <a-breadcrumb-item><a @click="$router.push('/')">首页</a></a-breadcrumb-item>
            <a-breadcrumb-item>文章正文</a-breadcrumb-item>
          </a-breadcrumb>
        </div>

        <div class="article-wrapper" v-if="article">
          <div class="article-flex-container">
            <!-- Left Column: Article Content -->
            <div class="article-main-col">
              <!-- Hero Section -->
              <div class="article-hero">
                <h1 class="article-title">{{ article.title }}</h1>
                <div class="hero-meta-row">
                   <div class="meta-item author-item">
                      <a-avatar :size="24" :style="{ backgroundColor: getRandomColor(article.id) }">
                        {{ (article.author || 'M')[0] }}
                      </a-avatar>
                      <span class="author-name">{{ article.author || 'Mayday Author' }}</span>
                   </div>
                   <span class="divider">·</span>
                   <div class="meta-item">
                      <span class="publish-time">{{ formatDate(article.publishTime) }}</span>
                   </div>
                   <span class="divider">·</span>
                   <div class="meta-item">
                      <span>阅读 {{ Math.floor(Math.random() * 5000) + 500 }}</span>
                   </div>
                   <span class="divider" v-if="article.sourceSite">·</span>
                   <a-tag color="blue" v-if="article.sourceSite" style="border:none">{{ article.sourceSite }}</a-tag>
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
            <div class="widget-header">
               <span>关于作者</span>
               <a-tag color="blue">签约作者</a-tag>
            </div>
            <div class="widget-body">
              <div class="author-row">
                <div class="avatar-lg" :style="{ background: article ? getRandomColor(article.id) : '#ccc' }">
                  {{ article ? (article.author || 'M')[0] : 'M' }}
                </div>
                <div class="info">
                  <div class="name">{{ article ? article.author : 'Loading' }}</div>
                  <div class="role">
                     <span class="badage">Lv4</span>
                     <span>全栈开发工程师</span>
                  </div>
                </div>
                <a-button type="primary" size="small" shape="round" ghost>+ 关注</a-button>
              </div>
              <div class="author-stat">
                 <div class="stat-item">
                    <div class="num">128</div>
                    <div class="label">文章</div>
                 </div>
                 <div class="stat-item">
                    <div class="num">2.3k</div>
                    <div class="label">阅读</div>
                 </div>
                 <div class="stat-item">
                    <div class="num">566</div>
                    <div class="label">粉丝</div>
                 </div>
              </div>
            </div>
          </div>
          <div class="widget-card recommend-widget" v-if="recommendList.length > 0">
            <div class="widget-header">
               <span>热门推荐</span>
               <a-button type="link" size="small" style="padding: 0">更多 <arrow-right-outlined /></a-button>
            </div>
            <div class="side-list">
              <div class="side-item" v-for="item in recommendList.slice(0, 5)" :key="item.id" @click="goToDetail(item.id)">
                <div class="side-thumb" v-if="item.coverImage">
                  <img :src="item.coverImage"/>
                </div>
                <div class="side-content">
                  <div class="side-title">{{ item.title }}</div>
                  <div class="side-meta">
                     <span class="date">{{ formatDate(item.publishTime) }}</span>
                     <span class="view"><eye-outlined /> {{ Math.floor(Math.random() * 1000) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>
    </div>
    <a-float-button
      v-if="showBackTop"
      type="primary"
      @click="scrollToTop"
      :style="{ right: '24px', bottom: '100px', zIndex: 999 }"
    >
      <template #icon>
        <vertical-align-top-outlined />
      </template>
    </a-float-button>
  </div>
</template>

<script setup lang="ts">
import {ref, onMounted, watch, computed, onUnmounted} from 'vue';
import {useRoute, useRouter} from 'vue-router';
import {
  LikeOutlined, StarOutlined, VerticalAlignTopOutlined,
  ShareAltOutlined, ExportOutlined, EyeOutlined, PictureOutlined
} from '@ant-design/icons-vue';
import {fetchPortalArticleDetail, fetchPortalArticleList} from '../../../api/frontend/portal';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
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
const scrollRef = ref<HTMLElement | null>(null);
const sentinelRef = ref<HTMLElement | null>(null);

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
const showBackTop = ref(false);
let observer: IntersectionObserver | null = null;
let scrollTimeout: any = null;
const isMounted = ref(false);

let rafId: number | null = null;

const scrollToTop = () => {
  const container = scrollRef.value;
  if (!container) return;
  
  // Cancel any ongoing scroll to avoid conflict
  if (rafId !== null) cancelAnimationFrame(rafId);
  
  const start = container.scrollTop;
  const startTime = Date.now();
  const duration = 800; // Increased to 800ms for improved "feel"

  const animateScroll = () => {
    const now = Date.now();
    const time = Math.min(1, ((now - startTime) / duration));
    
    // EaseInOutCubic - smoother start and end
    const ease = time < 0.5 
      ? 4 * time * time * time 
      : 1 - Math.pow(-2 * time + 2, 3) / 2;

    container.scrollTop = start * (1 - ease);

    if (time < 1) {
      rafId = requestAnimationFrame(animateScroll);
    } else {
      rafId = null;
    }
  };

  rafId = requestAnimationFrame(animateScroll);
};

const scrollToSection = (id: string) => {
  activeHeadingId.value = id;
  isScrollingByClick.value = true;
  const el = document.getElementById(id);
  const container = scrollRef.value;
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

const setupObserver = () => {
  if (observer) observer.disconnect();
  
  // Sentinel Observer for Back-to-Top visibility
  const sentinelCallback = (entries: IntersectionObserverEntry[]) => {
      entries.forEach(entry => {
          // If sentinel is visible/intersecting, we are at top -> Hide button
          // If sentinel is NOT visible, we scrolled down -> Show button
          showBackTop.value = !entry.isIntersecting;
      });
  };
  
  const sentinelObserver = new IntersectionObserver(sentinelCallback, {
      root: scrollRef.value,
      threshold: 0
  });
  
  if (sentinelRef.value) sentinelObserver.observe(sentinelRef.value);

  // TOC Observer
  const tocCallback = (entries: IntersectionObserverEntry[]) => {
    if (isScrollingByClick.value) return;
    
    for (const entry of entries) {
      if (entry.isIntersecting && entry.intersectionRatio > 0) {
         activeHeadingId.value = entry.target.id;
      }
    }
  };

  observer = new IntersectionObserver(tocCallback, {
    root: scrollRef.value,
    rootMargin: '-60px 0px -80% 0px',
    threshold: 0
  });

  tocList.value.forEach(item => {
    const el = document.getElementById(item.id);
    if (el) observer?.observe(el);
  });
};

// Re-observe when TOC list changes (e.g., article loaded)
watch(() => tocList.value, () => {
   // Wait for DOM update
   setTimeout(setupObserver, 100);
});

onMounted(() => {
  isMounted.value = true;
  if (route.params.id) {
    loadDetail(Number(route.params.id));
    loadRecommendations();
  }
  // Removed scroll listener
});

onUnmounted(() => {
  // scrollRef.value?.removeEventListener('scroll', handleScroll);<bos>
  if (observer) observer.disconnect();
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
  will-change: transform; /* Force hardware acceleration */
}

.scroll-sentinel {
  width: 100%;
  height: 1px;
  pointer-events: none;
  opacity: 0;
}

.main-feed {
  flex: 1;
  min-width: 0; /* Critical for preventing flex overflow */
}

.sidebar-right {
  width: 300px;
  flex-shrink: 0;
  position: sticky;
  top: 0;
  height: fit-content;
}

.breadcrumb-bar {
  margin-bottom: 20px;
}

.article-wrapper {
  background: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.article-hero {
  border-bottom: 1px solid #e4e6eb;
  padding-bottom: 20px;
  margin-bottom: 30px;
}

.article-title {
  font-size: 32px;
  font-weight: 700;
  color: #1d1d1f;
  margin-bottom: 20px;
  line-height: 1.4;
}

.hero-meta-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 14px;
  color: #8a919f;
  margin-bottom: 0;
}

.meta-item {
  display: flex;
  align-items: center;
}

.author-item {
  color: #333;
  font-weight: 500;
  gap: 8px;
  cursor: pointer;
  transition: color 0.2s;
}

.author-item:hover {
  color: #1677ff;
}

.author-item .author-name {
  font-size: 14px;
}

.divider {
  color: #e5e6eb;
  margin: 0 4px;
}

.article-flex-container {
  display: flex;
  gap: 30px;
}

.article-main-col {
  flex: 1;
  min-width: 0;
  overflow: hidden; /* Failsafe: Hide anything that still manages to overflow */
}

.article-toc-col {
  width: 220px;
  border-left: 1px solid #f0f0f0;
  padding-left: 15px;
}

.internal-toc-widget {
  position: sticky;
  top: 20px;
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
  word-break: break-word; /* Prevent long words/URLs from breaking layout */
  overflow-wrap: break-word;
}

.detail-body :deep(h1), .detail-body :deep(h2), .detail-body :deep(h3) {
  margin-top: 30px;
  margin-bottom: 15px;
  font-weight: bold;
}

.detail-body :deep(p) {
  margin-bottom: 16px;
}

/* Nuclear Option: Constrain ALL media and containers */
.detail-body :deep(img),
.detail-body :deep(video),
.detail-body :deep(iframe),
.detail-body :deep(figure),
.detail-body :deep(div) {
  max-width: 100% !important;
  box-sizing: border-box !important;
}

.detail-body :deep(img) {
  height: auto !important;
  border-radius: 8px;
  margin: 20px 0;
  display: block;
  object-fit: contain; /* Ensure image scales proportionally within container */
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
  padding: 0;
  cursor: default;
}

.widget-card.author-widget:hover {
  transform: none;
}

.widget-header {
  font-weight: 600;
  font-size: 16px;
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.widget-body {
  padding: 20px;
}

.author-row {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.avatar-lg {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: bold;
  margin-right: 12px;
  flex-shrink: 0;
}

.info {
  flex: 1;
  min-width: 0;
  margin-right: 8px;
}

.name {
  font-weight: 600;
  font-size: 16px;
  color: #333;
  margin-bottom: 4px;
}

.role {
  font-size: 12px;
  color: #86909c;
  display: flex;
  align-items: center;
  gap: 6px;
}

.badage {
  background: #e6f7ff;
  color: #1677ff;
  padding: 0 4px;
  border-radius: 2px;
  font-weight: bold;
}

.author-stat {
  display: flex;
  justify-content: space-around;
  text-align: center;
}

.stat-item .num {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.stat-item .label {
  font-size: 12px;
  color: #86909c;
  margin-top: 4px;
}

.side-list {
  padding: 16px 20px;
}

.side-item {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
  cursor: pointer;
}

.side-item:last-child {
  margin-bottom: 0;
}

.side-thumb {
  width: 80px;
  height: 60px;
  border-radius: 4px;
  overflow: hidden;
  flex-shrink: 0;
}

.side-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.side-item:hover .side-thumb img {
  transform: scale(1.1);
}

.side-content {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.side-title {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  line-height: 1.4;
  margin-bottom: 6px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  transition: color 0.3s;
}

.side-item:hover .side-title {
  color: #1677ff;
}

.side-meta {
  font-size: 12px;
  color: #999;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
