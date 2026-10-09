<template>
  <div class="dashboard">
    <!-- ==================== 统计卡片 ==================== -->
    <el-row :gutter="16" v-loading="statsLoading">
      <el-col v-for="card in statCards" :key="card.label" :xs="24" :sm="12" :md="8" :lg="4">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-icon" :style="{ background: card.color }">
            <el-icon :size="22" color="#fff"><component :is="card.icon" /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-value">
              {{ card.value }}<span v-if="card.suffix" class="suffix">{{ card.suffix }}</span>
            </div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="main-row">
      <!-- ==================== 最新订单 ==================== -->
      <el-col :xs="24" :lg="16">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>最新订单</span>
              <el-button type="primary" link :icon="Refresh" @click="fetchRecentOrders">刷新</el-button>
            </div>
          </template>
          <el-table :data="recentOrders" v-loading="orderLoading" border stripe size="small">
            <el-table-column label="订单号" min-width="200" show-overflow-tooltip>
              <template #default="{ row }">
                <el-link type="primary" :underline="false" @click="goOrder(row.orderSn)">{{ row.orderSn }}</el-link>
              </template>
            </el-table-column>
            <el-table-column label="金额" width="120" align="right">
              <template #default="{ row }">
                <span class="amount">{{ formatMoney(row.totalAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100" align="center">
              <template #default="{ row }">
                <el-tag :type="orderStatusTagType(row.status)" size="small">
                  {{ orderStatusLabel(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="商品数" width="90" align="center">
              <template #default="{ row }">{{ row.items?.length ?? 0 }}</template>
            </el-table-column>
            <el-table-column label="创建时间" width="170" align="center">
              <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="90" align="center">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="goOrder(row.orderSn)">去处理</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!orderLoading && !recentOrders.length" description="暂无订单数据" />
        </el-card>
      </el-col>

      <!-- ==================== 状态分布 + 快捷入口 ==================== -->
      <el-col :xs="24" :lg="8">
        <el-card shadow="never" class="side-card">
          <template #header><span>待处理事项</span></template>
          <ul class="todo-list">
            <li>
              <span class="todo-label">待审核商品</span>
              <el-tag type="warning" size="small">{{ stats.pendingVerify }}</el-tag>
            </li>
            <li>
              <span class="todo-label">审核驳回商品</span>
              <el-tag type="danger" size="small">{{ stats.rejected }}</el-tag>
            </li>
            <li>
              <span class="todo-label">已下架商品</span>
              <el-tag type="info" size="small">{{ stats.unpublished }}</el-tag>
            </li>
            <li>
              <span class="todo-label">最新订单中待支付</span>
              <el-tag type="warning" size="small">{{ unpaidInRecent }}</el-tag>
            </li>
          </ul>
        </el-card>

        <el-card shadow="never" class="side-card">
          <template #header><span>快捷入口</span></template>
          <div class="quick-actions">
            <el-button type="primary" plain :icon="Plus" @click="router.push('/product')">新增 / 管理商品</el-button>
            <el-button type="success" plain :icon="ShoppingCart" @click="router.push('/order')">下单 / 管理订单</el-button>
          </div>
          <el-alert
            type="info"
            :closable="false"
            show-icon
            class="tip"
            title="数据来源：/api/product/list 的 total 与 /api/order/list 的前 8 条（订单列表接口不返回总条数）。"
          />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Goods, Plus, Refresh, Sell, ShoppingCart, Tickets, TrendCharts, WarningFilled } from '@element-plus/icons-vue'
import { getProductList } from '@/api/product'
import { getOrderList } from '@/api/order'
import {
  ORDER_STATUS,
  formatDateTime,
  formatMoney,
  normalizePageResult,
  orderStatusLabel,
  orderStatusTagType
} from '@/utils/constants'

const router = useRouter()

const statsLoading = ref(false)
const orderLoading = ref(false)
const recentOrders = ref([])

const stats = reactive({
  total: 0,
  published: 0,
  unpublished: 0,
  pendingVerify: 0,
  verified: 0,
  rejected: 0,
  orderTotal: 0
})

const statCards = computed(() => [
  { label: '商品总数', value: stats.total, color: '#409eff', icon: Goods },
  { label: '已上架', value: stats.published, color: '#67c23a', icon: Sell },
  { label: '已下架', value: stats.unpublished, color: '#909399', icon: Tickets },
  { label: '待审核', value: stats.pendingVerify, color: '#e6a23c', icon: WarningFilled },
  { label: '审核驳回', value: stats.rejected, color: '#f56c6c', icon: WarningFilled },
  { label: '订单总数', value: stats.orderTotal, suffix: '（估算）', color: '#9254de', icon: TrendCharts }
])

const unpaidInRecent = computed(
  () => recentOrders.value.filter((i) => i.status === ORDER_STATUS.UNPAID).length
)

/** 商品统计：复用列表接口，pageSize=1 只取 total，开销极小 */
async function fetchStats() {
  statsLoading.value = true
  const ask = (params) => getProductList({ pageNum: 1, pageSize: 1, ...params })
  try {
    const results = await Promise.allSettled([
      ask({}),
      ask({ publishStatus: 1 }),
      ask({ publishStatus: 0 }),
      ask({ verifyStatus: 0 }),
      ask({ verifyStatus: 1 }),
      ask({ verifyStatus: 2 })
    ])
    const readTotal = (r) =>
      r.status === 'fulfilled'
        ? normalizePageResult(r.value, { pageNum: 1, pageSize: 1 }).total
        : 0
    const [all, published, unpublished, pendingVerify, verified, rejected] = results.map(readTotal)
    Object.assign(stats, { total: all, published, unpublished, pendingVerify, verified, rejected })
  } finally {
    statsLoading.value = false
  }
}

/** 最新订单 + 订单总数（该接口返回数组，总数按“是否满页”估算） */
async function fetchRecentOrders() {
  orderLoading.value = true
  try {
    const data = await getOrderList({ pageNum: 1, pageSize: 8 })
    const page = normalizePageResult(data, { pageNum: 1, pageSize: 8 })
    recentOrders.value = page.list
    stats.orderTotal = page.total
  } catch (e) {
    recentOrders.value = []
    stats.orderTotal = 0
  } finally {
    orderLoading.value = false
  }
}

function goOrder(orderSn) {
  router.push({ path: '/order', query: { orderSn } })
}

onMounted(() => {
  fetchStats()
  fetchRecentOrders()
})
</script>

<style scoped>
.stat-card :deep(.el-card__body) {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px;
}

.stat-icon {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 46px;
}

.stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}

.stat-value .suffix {
  font-size: 12px;
  font-weight: 400;
  color: #909399;
  margin-left: 4px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-top: 2px;
}

.main-row {
  margin-top: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.side-card {
  margin-bottom: 16px;
}

.todo-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.todo-list li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 0;
  border-bottom: 1px dashed #ebeef5;
}

.todo-list li:last-child {
  border-bottom: none;
}

.todo-label {
  color: #606266;
}

.quick-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.quick-actions .el-button {
  margin-left: 0;
}

.tip {
  margin-top: 14px;
}

.amount {
  color: #f56c6c;
  font-weight: 600;
}
</style>