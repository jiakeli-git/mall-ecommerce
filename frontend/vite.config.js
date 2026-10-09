import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 配置 @ 别名指向 src 目录
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5173,
    // 开发环境代理：将 /api 开头的请求转发到后端 SpringBoot
    proxy: {
      '/api': {
        target: 'http://localhost:8080', // 后端地址，按实际修改
        changeOrigin: true
      }
    }
  }
})
