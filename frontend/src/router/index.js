import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layout/index.vue'

/**
 * 路由表
 * 页面与后端接口的对应关系：
 *  /dashboard 数据概览 —— 复用 /api/product/list 的 total + /api/order/list 前 N 条
 *  /product   商品管理 —— /api/product/*
 *  /order     订单管理 —— /api/order/*
 */
const routes = [
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '数据概览', icon: 'Odometer' }
      },
      {
        path: 'product',
        name: 'Product',
        component: () => import('@/views/product/ProductList.vue'),
        meta: { title: '商品管理', icon: 'Goods' }
      },
      {
        path: 'order',
        name: 'Order',
        component: () => import('@/views/order/OrderList.vue'),
        meta: { title: '订单管理', icon: 'Tickets' }
      }
    ]
  },
  // 兜底：访问不存在的地址回到概览页
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 同步浏览器标签标题
router.afterEach((to) => {
  const title = to.meta?.title
  document.title = title ? `${title} · 电商后台管理系统` : '电商后台管理系统'
})

export default router