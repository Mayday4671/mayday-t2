import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import path from "path";

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "src"),
    },
  },
  server: {
    proxy: {
      // 代理 API 请求到后端
      "/api": {
        target: "http://localhost:9002",
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api/, ""),
      },
      // 代理爬虫图片静态资源请求到后端
      "/crawler-images": {
        target: "http://localhost:9002",
        changeOrigin: true,
      },
      // 代理上传文件静态资源
      "/upload": {
        target: "http://localhost:9002",
        changeOrigin: true,
      },
    },
  },
});

