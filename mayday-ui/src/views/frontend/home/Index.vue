<template>
  <div class="portal-container">
    <!-- Header -->
    <header class="portal-header">
      <div class="header-content">
        <div class="logo-area">
          <span class="logo-text">Mayday</span>
          <span class="logo-sub">Insight</span>
        </div>
        
        <nav class="main-nav">
          <a class="nav-item active">首页</a>
          <a class="nav-item">沸点</a>
          <a class="nav-item">课程</a>
          <a class="nav-item">直播</a>
        </nav>

        <div class="search-area">
          <div class="search-input-wrapper">
             <input 
              v-model="queryParams.title" 
              @keyup.enter="handleSearch"
              type="text" 
              class="search-input" 
              placeholder="搜索文章、话题..." 
            />
            <search-outlined class="search-icon" />
          </div>
        </div>

        <div class="user-actions">
           <a-button type="primary" class="publish-btn">创作者中心</a-button>
        </div>
      </div>
    </header>

    <!-- Main Layout -->
    <div class="portal-layout">
      
      <!-- Left Sidebar (Collapsible) -->
      <aside class="sidebar-left">
        <div class="nav-card">
          <a class="side-nav-item active"><fire-outlined /> 综合推荐</a>
          <a class="side-nav-item"><eye-outlined /> 关注</a>
          <a class="side-nav-item"><rocket-outlined /> 后端</a>
          <a class="side-nav-item"><appstore-outlined /> 前端</a>
          <a class="side-nav-item"><android-outlined /> Android</a>
          <a class="side-nav-item"><apple-outlined /> iOS</a>
          <a class="side-nav-item"><code-outlined /> 人工智能</a>
        </div>
        
      
      </aside>

      <!-- Center Content (Grid Feed) -->
      <main class="main-feed">
        <!-- Feed Tabs -->
        <div class="feed-tabs-card">
          <div class="tab-item active">推荐</div>
          <div class="tab-item">最新</div>
          <div class="tab-item">热榜</div>
        </div>

        <!-- Article Grid -->
        <div class="article-grid">
          <div v-if="loading && articleList.length === 0" class="loading-state">
            <a-spin size="large" />
          </div>

          <template v-else>
             <div 
              v-for="article in articleList" 
              :key="article.id" 
              class="article-card"
              @click="openDetail(article)"
            >
              <!-- Cover Image Area -->
              <div class="card-cover">
                 <img v-if="getArticleImage(article)" :src="getArticleImage(article)" loading="lazy" />
                 <div v-else class="cover-placeholder" :style="{ background: getRandomColor(article.id) }">
                    {{ (article.title || 'M')[0] }}
                 </div>
              </div>

              <!-- Card Content -->
              <div class="card-body">
                 <h3 class="card-title" :title="article.title">{{ article.title }}</h3>
                 
                 <div class="card-meta">
                    <span class="author">
                       <span class="avatar-mini" :style="{ background: getRandomColor(article.id) }">{{ (article.author || 'M')[0] }}</span> 
                       {{ article.author || 'Mayday' }}
                    </span>
                    <span class="time">{{ formatDate(article.publishTime) }}</span>
                 </div>
                 
                 <div class="card-footer">
                    <span class="tag">{{ article.sourceSite || '全网' }}</span>
                    <div class="actions">
                       <span><eye-outlined /> {{ Math.floor(Math.random() * 1000) }}</span>
                    </div>
                 </div>
              </div>
            </div>
          </template>
        </div>
          
        <div class="pagination-wrapper" v-if="pagination.total > 0">
            <a-pagination
              v-model:current="pagination.current"
              v-model:pageSize="pagination.pageSize"
              :total="pagination.total"
              :show-total="(total: number) => `共 ${total} 条`"
              :page-size-options="['12', '24', '36', '48']"
              show-size-changer
              @change="handlePageChange"
              align="center"
            />
        </div>
      </main>

      <!-- Right Sidebar -->
      <aside class="sidebar-right">
        <!-- Login Card -->
        <div class="widget-card login-widget">
           <div class="login-bg-decor"></div>
           <div class="login-content">
             <div class="widget-title">Mayday Insight</div>
             <p class="slogan">连接全球分布算力，探索技术无限可能</p>
             <div class="btn-group">
                <a-button type="primary" class="login-action-btn" block @click="$router.push('/login')">立即登录</a-button>
                <a-button class="register-action-btn" block ghost>注册账号</a-button>
             </div>
           </div>
        </div>

        <!-- Hot Topics -->
        <div class="widget-card hot-widget">
           <div class="widget-header">
             <span class="title">今日热榜</span>
             <span class="more"><right-outlined /></span>
           </div>
           <div class="hot-list">
              <div class="hot-item" v-for="i in 5" :key="i">
                 <span class="rank-badge" :class="'rank-' + i">{{ i }}</span>
                 <span class="text">DeepSeek R1 发布性能报告：超越 Open1o 预览版</span>
                 <span class="hot-icon" v-if="i <= 2"><fire-outlined /></span>
              </div>
           </div>
        </div>
      </aside>

    </div>
  </div>
</template>



<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { 
  SearchOutlined, FireOutlined, EyeOutlined, RocketOutlined,
  AppstoreOutlined, AndroidOutlined, AppleOutlined, CodeOutlined,
  RightOutlined, LinkOutlined
} from '@ant-design/icons-vue';
import { fetchPortalArticleList } from '../../../api/frontend/portal';
import dayjs from 'dayjs';
import relativeTime from 'dayjs/plugin/relativeTime';
import 'dayjs/locale/zh-cn';

// Import local cover images
import cover1 from '../../../assets/images/covers/cover_1.png';
import cover2 from '../../../assets/images/covers/cover_2.png';
import cover3 from '../../../assets/images/covers/cover_3.png';
import cover4 from '../../../assets/images/covers/cover_4.png';

dayjs.extend(relativeTime);
dayjs.locale('zh-cn');

const localCovers = [cover1, cover2, cover3, cover4];

interface Article {
  id: number;
  title: string;
  summary?: string;
  content?: string;
  author?: string;
  publishTime?: string;
  sourceSite?: string;
  url?: string;
  images?: string[]; 
}

const loading = ref(false);
const articleList = ref<Article[]>([]);
const router = useRouter();

const pagination = reactive({ current: 1, pageSize: 12, total: 0 });
const queryParams = reactive({ title: '' });

// Helper: Get article image (Priority: Cover -> First Content Image -> Local Fallback)
const getArticleImage = (article: Article): string => {
  // 1. Prioritize explicit backend cover field
  if ((article as any).coverImage) return (article as any).coverImage;

  // 2. Extract first img src from HTML content
  const html = article.content || article.summary || '';
  const imgMatch = html.match(/<img[^>]+src="([^">]+)"/);
  if (imgMatch && imgMatch[1]) {
    return imgMatch[1];
  }

  // 3. Fallback: Local random image based on ID
  // TS might think array access is undefined, so we default to empty string or assert
  const fallback = localCovers[0] || ''; 
  if (!article.id) return fallback;
  const index = Number(article.id) % localCovers.length;
  return localCovers[index] || fallback;
};


// Helper: Random pastel color for placeholder
const getRandomColor = (id: number = 0) => {
  const colors = ['#FF9AA2', '#FFB7B2', '#FFDAC1', '#E2F0CB', '#B5EAD7', '#C7CEEA'];
  return colors[id % colors.length];
};

const fetchArticles = async () => {
  loading.value = true;
  try {
    const res: any = await fetchPortalArticleList({
      current: pagination.current,
      pageSize: pagination.pageSize,
      title: queryParams.title
    });
    if (res && res.records) {
      articleList.value = res.records;
      pagination.total = res.totalRow || 0;
      // Scroll to top of feed
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  } catch (e) { console.error(e); } finally { loading.value = false; }
};

const handleSearch = () => { pagination.current = 1; fetchArticles(); };
const handlePageChange = () => { fetchArticles(); };

const openDetail = (article: Article) => {
  router.push('/article/' + article.id);
};

const formatDate = (dateStr?: string) => dateStr ? dayjs(dateStr).fromNow() : '刚刚';

onMounted(() => fetchArticles());
</script>

<style scoped>
:root {
  --primary-color: #1677ff;
  --bg-body: #f0f2f5;
}

.portal-container {
  min-height: 100vh;
  background-color: #f0f2f5;
  color: #1d1d1f;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Arial, sans-serif;
  overflow-x: hidden;
}

/* Wide Header */
.portal-header {
  height: 60px;
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  position: sticky;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
}
.header-content {
  width: 100%;
  /* Removed max-width limit, used padding instead */
  padding: 0 24px;
  display: flex;
  align-items: center;
}

.logo-area { font-size: 1.4rem; font-weight: bold; margin-right: 40px; color: #1677ff; }
.main-nav { display: flex; gap: 24px; margin-right: auto; }
.nav-item { color: #515767; font-size: 1rem; cursor: pointer; }
.nav-item.active { color: #1677ff; }

.search-input-wrapper { background: #f2f3f5; border-radius: 4px; padding: 0 12px; }
.search-input { border: none; background: transparent; padding: 8px 0; outline: none; width: 260px;}

/* Full Width Layout */
.portal-layout {
  /* Removed max-width */
  width: 100%;
  padding: 0 24px; /* Matches user request ~20px sides */
  margin: 20px auto;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.sidebar-left { width: 180px; flex-shrink: 0; position: sticky; top: 80px; }
.sidebar-right { width: 280px; flex-shrink: 0; position: sticky; top: 80px; }

/* Grid Feed */
.main-feed { flex: 1; min-width: 0; }

.feed-tabs-card {
  background: #fff;
  padding: 16px 20px;
  display: flex;
  gap: 24px;
  border-bottom: 1px solid #f0f0f0;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 1px 2px 0 rgba(0,0,0,0.05);
}
.tab-item:hover, .tab-item.active { color: #1677ff; font-weight: bold; cursor: pointer; }

/* CSS Grid for Cards */
.article-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr)); /* Responsive columns */
  gap: 20px;
}

.article-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 1px 3px rgba(0,0,0,0.05); /* Soft shadow */
  transition: all 0.3s ease;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%; /* Equal height */
}
.article-card:hover { 
  transform: translateY(-4px); 
  box-shadow: 0 8px 20px rgba(0,0,0,0.12);
}

.card-cover {
  width: 100%;
  height: 140px; /* Increased height */
  background: #f0f2f5;
  position: relative;
  overflow: hidden;
}
.card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.5s;
}
.article-card:hover .card-cover img { transform: scale(1.05); }

.cover-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 3rem;
  color: rgba(255,255,255,0.8);
  font-weight: bold;
}

.card-body {
  padding: 14px;
  flex: 1;
  display: flex;
  flex-direction: column;
}
.card-title {
  font-size: 1rem;
  color: #1d1d1f;
  margin-bottom: 8px;
  font-weight: 600;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-meta {
  margin-top: auto; /* Push to bottom */
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #86909c;
  margin-bottom: 12px;
}
.author { display: flex; align-items: center; gap: 6px; }
.avatar-mini {
  width: 20px; height: 20px; border-radius: 50%;
  background: #ccc; color: #fff; font-size: 0.7rem;
  display: flex; align-items: center; justify-content: center;
}

.card-footer {
  padding-top: 8px;
  border-top: 1px solid #f0f2f5;
  display: flex;
  justify-content: space-between;
  color: #8b949e;
  font-size: 0.8rem;
}
.tag {
  background: #f2f3f5;
  padding: 2px 6px;
  border-radius: 4px;
  color: #515767;
}

/* Sidebar Widgets */
.nav-card, .widget-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 20px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.05);
}
.side-nav-item { padding: 10px 16px; display: flex; gap: 10px; align-items: center; color: #515767; border-radius: 4px; transition: background 0.2s; cursor: pointer; }
.side-nav-item:hover, .side-nav-item.active { background: #eaf2ff; color: #1677ff; }

/* Left Sidebar Footer */
.nav-card-extra {
  padding: 16px;
  font-size: 12px;
  color: #8a919f;
  background: transparent;
}
.footer-links {
  display: flex; 
  flex-wrap: wrap; 
  align-items: center; 
  margin-bottom: 8px;
}
.ft-link { color: #8a919f; cursor: pointer; transition: color 0.2s; }
.ft-link:hover { color: #1677ff; }
.dot { margin: 0 4px; }
.copyright, .beian { line-height: 1.6; }

/* Right Sidebar - Login Widget */
.login-widget {
  padding: 0; /* Remove default padding */
  position: relative;
  overflow: hidden;
  background: white;
  min-height: 180px;
}
.login-bg-decor {
  height: 60px;
  background: linear-gradient(135deg, #e0f2fe 0%, #eef2ff 100%);
  border-radius: 8px 8px 0 0;
}
.login-content {
  padding: 0 20px 20px 20px;
  margin-top: -30px; /* Pull up */
  position: relative;
  z-index: 2;
}
.widget-title {
  font-size: 18px;
  font-weight: bold;
  color: #1d1d1f;
  margin-bottom: 8px;
}
.slogan {
  font-size: 13px;
  color: #86909c;
  margin-bottom: 20px;
  line-height: 1.4;
}
.btn-group { display: flex; gap: 12px; }
.login-action-btn { flex: 1; }
.register-action-btn { flex: 1; color: #1d1d1f; border-color: #d9d9d9; }

/* Hot Topics Refined */
.hot-list { display: flex; flex-direction: column; gap: 16px; margin-top: 10px; }
.hot-item { display: flex; align-items: flex-start; gap: 10px; font-size: 14px; cursor: pointer; line-height: 1.4; }
.hot-item:hover .text { color: #1677ff; }
.rank-badge {
  flex-shrink: 0;
  width: 18px; height: 18px;
  line-height: 18px; text-align: center;
  border-radius: 3px;
  font-size: 12px;
  font-weight: bold;
  background: #f2f3f5; color: #86909c;
  margin-top: 2px;
}
.rank-1 { background: #ff3b30; color: white; }
.rank-2 { background: #ff9500; color: white; }
.rank-3 { background: #ffcc00; color: white; }
.text { flex: 1; overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; line-clamp: 2; -webkit-box-orient: vertical; color: #333; }
.hot-icon { color: #ff3b30; font-size: 12px; margin-top: 2px; }

/* Header Auth */
.auth-links { display: flex; align-items: center; gap: 16px; font-size: 14px; color: #86909c; }
.login-link, .regsiter-link { cursor: pointer; transition: color 0.2s; }
.login-link:hover, .regsiter-link:hover { color: #1677ff; }
.divider { color: #e5e6eb; }

.load-more-btn { margin-top: 24px; height: 40px; }
.pagination-wrapper { margin-top: 24px; display: flex; justify-content: center; }

/* Responsive */
@media (max-width: 1200px) {
  .sidebar-right { display: none; }
}
@media (max-width: 768px) {
  .sidebar-left { display: none; }
  .portal-layout { padding: 0 12px; }
  .article-grid { grid-template-columns: 1fr; }
}
</style>
