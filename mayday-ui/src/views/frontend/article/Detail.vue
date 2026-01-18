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
           <a-button type="text" @click="$router.push('/')"><arrow-left-outlined /> 返回列表</a-button>
        </div>
      </div>
    </header>

    <!-- Main Layout -->
    <div class="portal-layout">
      
      <!-- Center Content (Article Detail) -->
      <main class="main-feed">
        <div class="article-wrapper" v-if="article">
          
          <!-- Hero Section -->
          <div class="article-hero">
             <div class="hero-meta">
                <a-tag color="blue" v-if="article.sourceSite">{{ article.sourceSite }}</a-tag>
                <span class="publish-time">{{ formatDate(article.publishTime) }}</span>
             </div>
             <h1 class="article-title">{{ article.title }}</h1>
             
             <div class="author-card">
                <div class="avatar" :style="{ background: getRandomColor(article.id) }">{{ (article.author || 'M')[0] }}</div>
                <div class="author-info">
                   <div class="author-name">{{ article.author || 'Mayday Author' }}</div>
                   <div class="author-desc">发布于 {{ article.sourceSite || '全网' }} · 阅读 {{ Math.floor(Math.random() * 5000) + 500 }}</div>
                </div>
                <a-button type="primary" shape="round" size="small" class="follow-btn"><plus-outlined /> 关注</a-button>
             </div>
          </div>

          <!-- Cover Image (Featured) -->
          <div class="featured-image" v-if="article.coverImage">
             <img :src="article.coverImage" alt="Cover" />
          </div>


          <!-- Content Body -->
          <div class="article-content" id="detail-content">
             <div class="detail-body typo" v-html="article.content || article.summary || '暂无正文内容'"></div>
          </div>
          
          <!-- Image Gallery -->
          <div class="gallery-section" id="detail-gallery" v-if="article.imageList && article.imageList.length > 0">
              <div class="section-divider">
                  <span><picture-outlined /> 图片画廊</span>
              </div>
              <div class="gallery-grid">
                  <div class="gallery-item" v-for="(img, index) in article.imageList" :key="index" @click="previewImage(img)">
                      <img :src="img" loading="lazy" />
                      <div class="gallery-overlay"><eye-outlined /></div>
                  </div>
              </div>
          </div>


          <!-- Article Footer -->
          <div class="article-footer">
              <div class="original-link" v-if="article.sourceSite">
                 <span>文章来源：</span>
                 <a v-if="article.url" :href="article.url" target="_blank" rel="noopener noreferrer">{{ article.sourceSite }} <export-outlined /></a>
                 <span v-else>{{ article.sourceSite }}</span>
              </div>
              <div class="interaction-bar">
                 <a-button shape="circle" size="large"><like-outlined /></a-button>
                 <a-button shape="circle" size="large"><star-outlined /></a-button>
                 <a-button shape="circle" size="large"><share-alt-outlined /></a-button>
              </div>
          </div>

        </div> <!-- End of article-wrapper -->

        <!-- Bottom Recommendations (Outside wrapper for better card effect) -->
        <div class="recommend-section" v-if="recommendList.length > 0">
            <div class="section-title">推荐阅读</div>
            <div class="recommend-grid">
               <div class="recommend-card widget-card" v-for="item in recommendList.slice(0, 3)" :key="item.id" @click="goToDetail(item.id)">
                  <div class="rc-cover" v-if="item.coverImage">
                     <img :src="item.coverImage" loading="lazy" />
                  </div>
                  <div class="rc-info">
                     <div class="rc-title">{{ item.title }}</div>
                     <div class="rc-meta">{{ formatDate(item.publishTime) }}</div>
                  </div>
               </div>
            </div>
        </div>
        
        <div v-if="loading" class="loading-state">
           <a-skeleton active paragraph="{ rows: 10 }" />
        </div>
         <div v-else-if="!article && !loading" class="error-state">
           <empty description="未找到该文章" />
           <a-button type="primary" @click="$router.push('/')">返回首页</a-button>
        </div>

      </main>

      <!-- Right Sidebar (Context) -->
      <aside class="sidebar-right">
        <div class="sticky-container">
            <!-- Author/Site Widget -->
            <div class="widget-card author-widget">
               <div class="widget-header">关于作者</div>
               <div class="widget-body">
                  <div class="author-row">
                      <div class="avatar-lg" :style="{ background: article ? getRandomColor(article.id) : '#ccc' }">
                        {{ article ? (article.author || 'M')[0] : 'M' }}
                      </div>
                      <div class="info">
                          <div class="name">{{ article ? article.author : 'Loading...' }}</div>
                          <div class="role">内容创作者</div>
                      </div>
                  </div>
                  <div class="stats-row">
                      <div class="stat-item"><div class="num">128</div><div class="label">文章</div></div>
                      <div class="stat-item"><div class="num">1.2k</div><div class="label">阅读</div></div>
                      <div class="stat-item"><div class="num">56</div><div class="label">粉丝</div></div>
                  </div>
               </div>
            </div>

            <!-- Promotion / Login -->
            <div class="widget-card promo-widget">
               <h3>Mayday Insight</h3>
               <p>探索技术无限可能，连接全球分布算力。</p>
               <a-button type="primary" block>下载 App</a-button>
            </div>
            
            <!-- Sidebar Recommendations -->
            <div class="widget-card recommend-widget" v-if="recommendList.length > 0">
               <div class="widget-header">热门推荐</div>
               <div class="side-list">
                  <div class="side-item" v-for="item in recommendList.slice(0, 5)" :key="item.id" @click="goToDetail(item.id)">
                     <div class="side-thumb" v-if="item.coverImage">
                        <img :src="item.coverImage" />
                     </div>
                     <div class="side-content">
                        <div class="side-title">{{ item.title }}</div>
                        <div class="side-date">{{ formatDate(item.publishTime) }}</div>
                     </div>
                  </div>
               </div>
            </div>
            
            <!-- TOC Placeholder -->
            <div class="widget-card toc-widget">
               <div class="widget-header">目录</div>
               <div class="toc-list">
                  <div class="toc-item" @click="scrollToSection('detail-content')">文章正文</div>
                  <div class="toc-item" v-if="article && article.imageList?.length" @click="scrollToSection('detail-gallery')">图片画廊</div>
               </div>
            </div>
        </div>
      </aside>

    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { 
  ArrowLeftOutlined, PlusOutlined, LikeOutlined, StarOutlined, 
  ShareAltOutlined, ExportOutlined, EyeOutlined, PictureOutlined
} from '@ant-design/icons-vue';
import { fetchPortalArticleDetail, fetchPortalArticleList } from '../../../api/frontend/portal';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';
import { Empty } from 'ant-design-vue';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const route = useRoute();
const router = useRouter(); // Re-added used router
const loading = ref(false);
const article = ref<any>(null);
const recommendList = ref<any[]>([]);

const getRandomColor = (id: number = 0) => {
  const colors = ['#FF9AA2', '#FFB7B2', '#FFDAC1', '#E2F0CB', '#B5EAD7', '#C7CEEA'];
  return colors[id % colors.length];
};

const formatDate = (dateStr?: string) => dateStr ? dayjs(dateStr).format('YYYY年MM月DD日 HH:mm') : '刚刚';

const loadDetail = async (id: number) => {
    loading.value = true;
    try {
        const res = await fetchPortalArticleDetail(id);
        article.value = res;
    } catch (e) {
        console.error(e);
    } finally {
        loading.value = false;
    }
}

const loadRecommendations = async () => {
    try {
       // Mocking random fetch by just fetching page 1. In real app, might randomize or use tags.
       const res: any = await fetchPortalArticleList({ current: 1, pageSize: 6, title: '' });
       recommendList.value = res.records || [];
    } catch (e) {
       console.error(e);
    }
}

const previewImage = (src: string) => {
    window.open(src, '_blank');
};

const scrollToSection = (id: string) => {
    const el = document.getElementById(id);
    if (el) {
        el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
};

const goToDetail = (id: number) => {
   router.push(`/article/${id}`);
};

// Watch route param change to reload data when clicking recommendations
watch(() => route.params.id, (newId) => {
   if (newId) {
      loadDetail(Number(newId));
      window.scrollTo(0, 0);
   }
});

onMounted(() => {
    if (route.params.id) {
       loadDetail(Number(route.params.id));
       loadRecommendations();
    }
});
</script>

<style scoped>
:root {
  --primary-color: #1677ff;
  --text-main: #1d1d1f;
  --text-secondary: #86909c;
  --bg-body: #f7f8fa;
}

.portal-container {
  min-height: 100vh;
  background-color: #f0f2f5; /* Distinct gray background */
  color: var(--text-main);
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
  width: 100%;
}

/* Header */
.portal-header {
  height: 64px;
  background: rgba(255, 255, 255, 1); /* Solid white for better separation */
  box-shadow: 0 1px 4px rgba(0,21,41,0.08); /* Distinct shadow */
  border-bottom: none;
  position: sticky;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
}
.header-content {
  width: 100%;
  max-width: none; 
  padding: 0 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.logo-area { font-size: 1.5rem; font-weight: 800; color: #1677ff; letter-spacing: -0.5px; }
.logo-sub { color: #1d1d1f; margin-left: 4px; font-weight: 400; }
.main-nav { display: flex; gap: 32px; }
.nav-item { color: #515767; font-size: 1rem; cursor: pointer; font-weight: 500; transition: color 0.2s; }
.nav-item:hover { color: #1677ff; }

/* Layout */
.portal-layout {
  max-width: none; 
  width: 100%;
  margin: 24px 0;
  display: flex;
  gap: 24px;
  padding: 0 20px;
  align-items: flex-start;
  box-sizing: border-box;
}

.main-feed {
    flex: 1;
    min-width: 0; 
}
.sidebar-right {
    width: 320px; /* Slightly wider */
    flex-shrink: 0;
}

/* Article Wrapper */
.article-wrapper {
    background: #fff;
    border-radius: 8px; /* Slightly sharper */
    padding: 40px;
    box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.16), 0 3px 6px 0 rgba(0, 0, 0, 0.12), 0 5px 12px 4px rgba(0, 0, 0, 0.09); /* Stronger shadow */
}

/* Hero */
.article-hero { margin-bottom: 32px; }
.hero-meta { margin-bottom: 16px; display: flex; gap: 12px; align-items: center; }
.publish-time { color: #86909c; font-size: 0.9rem; }
.article-title {
    font-size: 2.2rem;
    font-weight: 700;
    line-height: 1.3;
    color: #1d1d1f;
    margin-bottom: 24px;
}

.author-card {
    display: flex;
    align-items: center;
}
.author-card .avatar {
    width: 44px; height: 44px; border-radius: 50%;
    background: #f0f0f0; color: #fff; font-size: 1.2rem;
    display: flex; align-items: center; justify-content: center;
    margin-right: 12px;
}
.author-info { flex: 1; }
.author-name { font-size: 1rem; font-weight: 600; color: #333; }
.author-desc { font-size: 0.85rem; color: #86909c; margin-top: 2px; }
.follow-btn { margin-left: 16px; }

/* Featured Image */
.featured-image {
    width: 100%;
    margin-bottom: 32px;
    border-radius: 8px;
    overflow: hidden;
    max-height: 500px;
}
.featured-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
}

/* Content Body (Typography) */
.detail-body {
    font-size: 1.05rem;
    line-height: 1.8;
    color: #333;
    letter-spacing: 0.2px;
}
.detail-body :deep(h1), .detail-body :deep(h2), .detail-body :deep(h3) {
    margin-top: 32px; margin-bottom: 16px; font-weight: 700; color: #111;
}
.detail-body :deep(p) { margin-bottom: 20px; text-align: justify; }
.detail-body :deep(img) {
    max-width: 100%;
    border-radius: 8px;
    margin: 20px 0;
    box-shadow: 0 4px 12px rgba(0,0,0,0.08);
}
.detail-body :deep(blockquote) {
    border-left: 4px solid #1677ff;
    padding-left: 16px;
    margin: 24px 0;
    color: #555;
    background: #f8faff;
    padding: 16px;
    border-radius: 0 8px 8px 0;
}
.detail-body :deep(pre), .detail-body :deep(code) {
    background: #f5f7fa;
    border-radius: 4px;
    font-family: 'JetBrains Mono', Consolas, monospace;
    font-size: 0.9em;
}

/* Gallery Section */
.gallery-section { margin-top: 48px; }
.section-divider {
    display: flex; align-items: center; justify-content: center;
    margin-bottom: 24px; 
    color: #1d1d1f; font-weight: 600; font-size: 1.2rem;
}
.section-divider span { background: #f2f3f5; padding: 4px 12px; border-radius: 20px; }

.gallery-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
    gap: 12px;
}
.gallery-item {
    position: relative;
    aspect-ratio: 1;
    border-radius: 8px;
    overflow: hidden;
    cursor: zoom-in;
    box-shadow: 0 2px 4px rgba(0,0,0,0.05);
}
.gallery-item img {
    width: 100%; height: 100%; object-fit: cover;
    transition: transform 0.4s ease;
}
.gallery-overlay {
    position: absolute; inset: 0;
    background: rgba(0,0,0,0.3);
    display: flex; align-items: center; justify-content: center;
    color: #fff; font-size: 1.5rem;
    opacity: 0; transition: opacity 0.3s;
}
.gallery-item:hover img { transform: scale(1.1); }
.gallery-item:hover .gallery-overlay { opacity: 1; }

/* Footer */
.article-footer {
    margin-top: 40px;
    padding-top: 32px;
    border-top: 1px solid #eee;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.original-link { color: #86909c; font-size: 0.9rem; }
.original-link a { color: #1677ff; }

.interaction-bar { display: flex; gap: 16px; }

/* Recommendations */
.recommend-section { margin-top: 32px; }
.section-title { font-weight: 700; font-size: 1.2rem; margin-bottom: 16px; color: #1d1d1f; border-left: 4px solid #1677ff; padding-left: 12px; }
.recommend-grid {
    display: grid;
    grid-template-columns: repeat(3, 1fr);
    gap: 20px;
}
.recommend-card {
    cursor: pointer;
    transition: all 0.3s;
    background: #fff;
    border-radius: 8px;
    padding: 0; /* Reset padding from widget-card if mixed */
    overflow: hidden;
    box-shadow: 0 1px 2px rgba(0,0,0,0.05);
    display: flex;
    flex-direction: column;
}
.recommend-card:hover { transform: translateY(-4px); box-shadow: 0 8px 20px rgba(0,0,0,0.1); }
.rc-cover {
    width: 100%;
    aspect-ratio: 16/10;
    background: #f0f0f0;
    overflow: hidden;
}
.rc-cover img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.3s; }
.recommend-card:hover .rc-cover img { transform: scale(1.05); }

.rc-info { padding: 16px; flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
.rc-title { font-weight: 600; color: #333; margin-bottom: 8px; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; font-size: 1rem; }
.rc-meta { color: #999; font-size: 0.8rem; }


/* Right Sidebar Widgets */
.sticky-container { position: sticky; top: 88px; }
.widget-card {
    background: #fff;
    border-radius: 8px;
    padding: 20px;
    margin-bottom: 16px;
    box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
.widget-header { font-weight: 600; margin-bottom: 16px; color: #1d1d1f; border-left: 3px solid #1677ff; padding-left: 10px; }

.author-row { display: flex; gap: 12px; align-items: center; margin-bottom: 16px; }
.avatar-lg { width: 48px; height: 48px; border-radius: 50%; color: #fff; background: #ddd; display: flex; align-items: center; justify-content: center; font-size: 1.2rem; }
.author-row .name { font-weight: 600; font-size: 1rem; }
.author-row .role { font-size: 0.8rem; color: #86909c; }

.stats-row { display: flex; justify-content: space-around; background: #f9f9f9; padding: 10px; border-radius: 6px; }
.stat-item { text-align: center; }
.stat-item .num { font-weight: 700; color: #1d1d1f; }
.stat-item .label { font-size: 0.75rem; color: #86909c; }

.promo-widget { background: linear-gradient(135deg, #1677ff 0%, #00d2ff 100%); color: #fff; text-align: center; }
.promo-widget h3 { color: #fff; margin-bottom: 8px; }
.promo-widget p { font-size: 0.9rem; opacity: 0.9; margin-bottom: 16px; }
.promo-widget .ant-btn { color: #1677ff; background: #fff; border: none; }

/* Side Recommendations */
.side-list { display: flex; flex-direction: column; gap: 16px; }
.side-item { display: flex; gap: 12px; cursor: pointer; }
.side-item:hover .side-title { color: #1677ff; }
.side-thumb { width: 80px; height: 60px; border-radius: 4px; overflow: hidden; flex-shrink: 0; background: #f0f0f0; }
.side-thumb img { width: 100%; height: 100%; object-fit: cover; }
.side-content { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
.side-title { font-size: 0.9rem; font-weight: 500; color: #333; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.side-date { font-size: 0.75rem; color: #bbb; }

.toc-list { display: flex; flex-direction: column; gap: 4px; }
.toc-item { padding: 8px 12px; border-radius: 4px; color: #555; cursor: pointer; transition: all 0.2s; font-size: 0.9rem; }
.toc-item:hover, .toc-item.active { background: #e6f4ff; color: #1677ff; }

/* Responsive */
@media (max-width: 992px) {
    .sidebar-right { display: none; }
    .portal-layout { padding: 0 16px; }
    .article-wrapper { padding: 24px; }
    .article-title { font-size: 1.8rem; }
    .recommend-grid { grid-template-columns: 1fr; }
}
</style>
