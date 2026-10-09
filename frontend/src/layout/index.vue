<template>
  <!-- 整体布局：左侧菜单 + 顶栏 + 内容区 -->
  <el-container class="layout">
    <!-- ==================== 侧边栏 ==================== -->
    <el-aside width="220px" class="aside">
      <div class="logo">
        <el-icon :size="22" color="#409EFF"><Shop /></el-icon>
        <span>电商后台管理系统</span>
      </div>
      <el-menu
        :default-active="activeMenu"
        router
        class="menu"
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container class="body">
      <!-- ==================== 顶栏 ==================== -->
      <el-header class="header" height="60px">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-tag type="success" effect="dark" round>管理端</el-tag>
          <el-avatar :size="30" class="avatar">A</el-avatar>
          <span class="admin-name">admin</span>
        </div>
      </el-header>

      <!-- ==================== 主内容区 ==================== -->
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Goods, Odometer, Shop, Tickets } from '@element-plus/icons-vue'

// 菜单与路由 meta 保持一致
const menus = [
  { path: '/dashboard', title: '数据概览', icon: Odometer },
  { path: '/product', title: '商品管理', icon: Goods },
  { path: '/order', title: '订单管理', icon: Tickets }
]

const route = useRoute()
const activeMenu = computed(() => route.path)
const currentTitle = computed(() => route.meta?.title || '')
</script>

<style scoped>
.layout {
  height: 100vh;
}

/* 侧边栏 */
.aside {
  background: #304156;
  overflow-x: hidden;
}

.logo {
  display: flex;
  align-items: center;
  gap: 8px;
  height: 60px;
  padding: 0 16px;
  color: #fff;
  font-size: 15px;
  font-weight: 600;
  letter-spacing: 0.5px;
  background: #2b3648;
  white-space: nowrap;
}

.menu {
  border-right: none;
  height: calc(100% - 60px);
}

/* 顶栏 */
.body {
  min-width: 0;
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e8eb;
  padding: 0 20px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  background: #409eff;
  font-size: 13px;
}

.admin-name {
  color: #606266;
  font-size: 13px;
}

/* 主内容区 */
.main {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>