import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      // 代理爬虫图片静态资源请求到后端
      "/crawler-images": {
        target: "http://localhost:9002",
        changeOrigin: true,
      },
    },
  },
});
