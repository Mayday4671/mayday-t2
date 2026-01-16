<template>
  <div class="dashboard">
    <!-- 统计卡片 -->
    <a-row :gutter="16">
      <a-col :xs="24" :sm="12" :md="6">
        <a-card class="stat-card stat-card-task" :loading="loading">
          <a-statistic
            title="爬虫任务"
            :value="stats.taskCount"
            :value-style="{ color: '#1890ff' }"
          >
            <template #prefix>
              <BugOutlined class="stat-icon" style="color: #1890ff" />
            </template>
            <template #suffix>
              <a-tag color="processing" style="margin-left: 8px">
                {{ stats.runningTaskCount }} 运行中
              </a-tag>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card class="stat-card stat-card-article" :loading="loading">
          <a-statistic
            title="文章总数"
            :value="stats.articleCount"
            :value-style="{ color: '#52c41a' }"
          >
            <template #prefix>
              <FileTextOutlined class="stat-icon" style="color: #52c41a" />
            </template>
            <template #suffix>
              <a-tag color="success" style="margin-left: 8px">
                +{{ stats.todayArticleCount }} 今日
              </a-tag>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card class="stat-card stat-card-image" :loading="loading">
          <a-statistic
            title="图片总数"
            :value="stats.imageCount"
            :value-style="{ color: '#faad14' }"
          >
            <template #prefix>
              <PictureOutlined class="stat-icon" style="color: #faad14" />
            </template>
            <template #suffix>
              <a-tag color="warning" style="margin-left: 8px">
                +{{ stats.todayImageCount }} 今日
              </a-tag>
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="12" :md="6">
        <a-card class="stat-card stat-card-log" :loading="loading">
          <a-statistic
            title="日志记录"
            :value="stats.logCount"
            :value-style="{ color: '#722ed1' }"
          >
            <template #prefix>
              <ProfileOutlined class="stat-icon" style="color: '#722ed1" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <!-- 图表区域 -->
    <a-row :gutter="16" style="margin-top: 16px">
      <!-- 任务状态分布 -->
      <a-col :xs="24" :md="8">
        <a-card :loading="loading" class="chart-card">
          <template #title>
            <PieChartOutlined style="margin-right: 8px; color: #1890ff" />
            任务状态分布
          </template>
          <div ref="pieChartRef" class="chart-container"></div>
          <a-empty
            v-if="!loading && (!stats.taskStatusDistribution || stats.taskStatusDistribution.length === 0)"
            description="暂无任务数据"
          />
        </a-card>
      </a-col>

      <!-- 近7天趋势 -->
      <a-col :xs="24" :md="16">
        <a-card :loading="loading" class="chart-card">
          <template #title>
            <LineChartOutlined style="margin-right: 8px; color: #52c41a" />
            近7天数据趋势
          </template>
          <div ref="lineChartRef" class="chart-container"></div>
          <a-empty
            v-if="!loading && (!stats.articleTrend || stats.articleTrend.length === 0)"
            description="暂无趋势数据"
          />
        </a-card>
      </a-col>
    </a-row>

    <!-- 快速入口 - 专业图标卡片 -->
    <a-row :gutter="[16, 16]" style="margin-top: 16px">
      <a-col :xs="12" :sm="8" :md="4" v-for="item in quickLinks" :key="item.path">
        <a-card
          class="quick-link-card"
          hoverable
          @click="$router.push(item.path)"
        >
          <div class="quick-link-content">
            <div class="quick-link-icon-wrapper" :style="{ background: item.bgColor }">
              <component :is="item.icon" class="quick-link-icon" :style="{ color: item.iconColor }" />
            </div>
            <div class="quick-link-title">{{ item.title }}</div>
          </div>
        </a-card>
      </a-col>
      <!-- 刷新数据按钮 -->
      <a-col :xs="12" :sm="8" :md="4">
        <a-card
          class="quick-link-card"
          hoverable
          @click="loadStats"
        >
          <div class="quick-link-content">
            <div class="quick-link-icon-wrapper" :style="{ background: '#f0f5ff' }">
              <ReloadOutlined 
                class="quick-link-icon" 
                :class="{ 'spin-animation': loading }"
                style="color: #1890ff" 
              />
            </div>
            <div class="quick-link-title">刷新数据</div>
          </div>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, markRaw, type Component } from "vue";
import { fetchDashboardStats, type DashboardStats } from "../../../api/admin/dashboard";
import { Pie, Area } from "@antv/g2plot"; // Reverted to Pie and Area
import {
  BugOutlined,
  FileTextOutlined,
  PictureOutlined,
  ProfileOutlined,
  PieChartOutlined,
  LineChartOutlined,
  ScheduleOutlined,
  UnorderedListOutlined,
  FileImageOutlined,
  CodeOutlined,
  GlobalOutlined,
  ReloadOutlined,
} from "@ant-design/icons-vue";

// 快速入口配置
interface QuickLink {
  title: string;
  icon: Component;
  path: string;
  bgColor: string;
  iconColor: string;
}

const quickLinks: QuickLink[] = [
  {
    title: "任务管理",
    icon: markRaw(ScheduleOutlined),
    path: "/crawler/task",
    bgColor: "#f0f5ff",
    iconColor: "#2f54eb",
  },
  {
    title: "文章管理",
    icon: markRaw(UnorderedListOutlined),
    path: "/article/list",
    bgColor: "#f6ffed",
    iconColor: "#52c41a",
  },
  {
    title: "图片管理",
    icon: markRaw(FileImageOutlined),
    path: "/article/image",
    bgColor: "#fff7e6",
    iconColor: "#fa8c16",
  },
  {
    title: "日志查看",
    icon: markRaw(CodeOutlined),
    path: "/crawler/log",
    bgColor: "#fff1f0",
    iconColor: "#f5222d",
  },
  {
    title: "代理配置",
    icon: markRaw(GlobalOutlined),
    path: "/crawler/proxy",
    bgColor: "#f9f0ff",
    iconColor: "#722ed1",
  },
];

// 状态
const loading = ref(false);
const stats = ref<DashboardStats>({
  taskCount: 0,
  runningTaskCount: 0,
  articleCount: 0,
  todayArticleCount: 0,
  imageCount: 0,
  todayImageCount: 0,
  logCount: 0,
  taskStatusDistribution: [],
  articleTrend: [],
  imageTrend: [],
});

// 图表引用
const pieChartRef = ref<HTMLDivElement>();
const lineChartRef = ref<HTMLDivElement>();

// 图表实例
let pieChart: Pie | null = null; // Reverted type
let lineChart: Area | null = null;

// 加载统计数据
const loadStats = async () => {
  loading.value = true;
  try {
    const data = await fetchDashboardStats();
    stats.value = data;
  } catch (e) {
    console.error("加载统计数据失败:", e);
  } finally {
    loading.value = false;
    // 必须在 loading 结束且 DOM更新后渲染图表，因为 a-card loading 会销毁 DOM
    await nextTick();
    renderPieChart();
    renderLineChart();
  }
};

// 渲染任务分布图 (回归经典精致的环形图)
const renderPieChart = () => {
  if (!pieChartRef.value) return;
  
  const pieData = stats.value.taskStatusDistribution;
  if (!pieData || pieData.length === 0) return;

  // 销毁旧图表
  if (pieChart) {
    pieChart.destroy();
    pieChart = null;
  }

  // 计算总数
  const total = pieData.reduce((acc, cur) => acc + cur.value, 0);

  // 使用 Pie (Donut模式)
  pieChart = new Pie(pieChartRef.value, {
    data: pieData,
    angleField: 'value',
    colorField: 'type',
    radius: 0.8,
    innerRadius: 0.64, // 黄金比例内径
    // 经典的商务配色，稳重且耐看
    color: ['#1890FF', '#13C2C2', '#2FC25B', '#FACC14', '#F04864', '#8543E0'],
    label: {
      type: 'inner',
      offset: '-50%',
      content: '{value}',
      style: {
        textAlign: 'center',
        fontSize: 14,
        fill: '#fff', // 白色文字
        opacity: 0.9,
      },
      autoRotate: false,
    },
    statistic: {
      title: {
        offsetY: -4,
        style: { fontSize: '24px', color: '#262626', fontWeight: 500 },
        // @ts-ignore
        customHtml: () => {
          return `<div><div style="font-size: 14px;color: #8c8c8c;margin-bottom:4px;">总任务</div><div style="font-size: 28px;color: #262626;font-weight: bold;line-height:1;">${total}</div></div>`;
        },
      },
      content: false,
    },
    legend: {
      position: 'bottom',
      itemHeight: 28, 
    },
    interactions: [{ type: 'element-selected' }, { type: 'element-active' }],
    // 添加一点阴影增加质感
    pieStyle: {
      lineWidth: 0,
    },
  });

  pieChart.render();
};

// 渲染折线图 (升级为渐变面积图)
const renderLineChart = () => {
  if (!lineChartRef.value) return;
  
  const articleData = stats.value.articleTrend || [];
  const imageData = stats.value.imageTrend || [];
  
  // 如果没有数据则不渲染
  if (articleData.length === 0 && imageData.length === 0) {
    return;
  }

  // 销毁旧图表
  if (lineChart) {
    lineChart.destroy();
    lineChart = null;
  }

  // 合并文章和图片趋势数据
  const trendData = [
    ...articleData.map((item) => ({
      date: item.date,
      value: item.value,
      category: "文章",
    })),
    ...imageData.map((item) => ({
      date: item.date,
      value: item.value,
      category: "图片",
    })),
  ];

  lineChart = new Area(lineChartRef.value, {
    data: trendData,
    xField: "date",
    yField: "value",
    seriesField: "category",
    smooth: true,
    // 使用更高级的半透明渐变填充
    areaStyle: () => {
      return {
        fill: 'l(270) 0:#ffffff 0.5:#7ec2f3 1:#1890ff', // 我们让 AntV 自动处理不同系列的颜色映射，或者这里如果不设置 fill，它会自动使用 series 颜色并降低透明度
        // 为了更好的多系列效果，通常只需 opacity 即可，AntV 会自动处理颜色
        fillOpacity: 0.6, 
      };
    },
    // 自定义系列颜色
    color: ["#5AD8A6", "#5B8FF9"], // 文章-绿色系, 图片-蓝色系
    point: {
      size: 4,
      style: {
        stroke: '#fff',
        lineWidth: 2,
      },
    },
    legend: {
      position: "top-right",
    },
    tooltip: {
      showMarkers: true,
    },
    yAxis: {
      grid: {
        line: {
          style: {
            lineDash: [4, 4],
            stroke: '#d9d9d9',
            opacity: 0.5
          }
        }
      }
    }
  });

  lineChart.render();
};

// 生命周期
onMounted(() => {
  loadStats();
});

onUnmounted(() => {
  // 销毁图表
  if (pieChart) {
    pieChart.destroy();
    pieChart = null;
  }
  if (lineChart) {
    lineChart.destroy();
    lineChart = null;
  }
});
</script>

<style scoped>
.dashboard {
  padding: 8px;
}

.stat-card {
  border-radius: 8px;
  transition: all 0.3s ease;
}

.stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.stat-card-task {
  border-top: 3px solid #1890ff;
}

.stat-card-article {
  border-top: 3px solid #52c41a;
}

.stat-card-image {
  border-top: 3px solid #faad14;
}

.stat-card-log {
  border-top: 3px solid #722ed1;
}

.stat-icon {
  font-size: 28px;
  margin-right: 8px;
}

.chart-card {
  border-radius: 8px;
  transition: box-shadow 0.3s ease;
}

.chart-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.10);
}

.chart-container {
  height: 320px; /* 增加一点高度 */
}

/* 快速入口样式 */
.quick-link-card {
  border-radius: 12px;
  transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
  border: 1px solid #f0f0f0;
}

.quick-link-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.12);
  border-color: transparent;
}

.quick-link-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.quick-link-icon-wrapper {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 12px;
  transition: all 0.3s ease;
}

.quick-link-card:hover .quick-link-icon-wrapper {
  transform: scale(1.1);
}

.quick-link-icon {
  font-size: 28px;
}

.quick-link-title {
  font-size: 14px;
  font-weight: 500;
  color: #262626;
}

/* 刷新动画 */
.spin-animation {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
