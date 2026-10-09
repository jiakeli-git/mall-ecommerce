<template>
  <div class="order-page">
    <!-- ==================== 搜索区 ==================== -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" @submit.prevent>
        <el-form-item label="订单号">
          <el-input
            v-model="query.orderSn"
            placeholder="订单号（后端为精确匹配）"
            clearable
            style="width: 260px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleSearch">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
          <el-button type="success" :icon="Plus" @click="handleCreateOrder">新建订单</el-button>
        </el-form-item>
      </el-form>

      <el-alert
        v-if="approximate"
        type="info"
        :closable="false"
        show-icon
        class="api-tip"
        title="后端 /api/order/list 返回的是 PageHelper 的 Page 对象（被序列化成数组，不带 total），分页总数为前端估算值。"
      />
    </el-card>

    <!-- ==================== 订单列表 ==================== -->
    <el-card shadow="never">
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column prop="id" label="订单ID" width="80" align="center" />

        <el-table-column label="订单号" min-width="210" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="handleDetail(row)">{{ row.orderSn }}</el-link>
          </template>
        </el-table-column>

        <el-table-column label="用户ID" width="80" align="center">
          <template #default="{ row }">{{ row.userId ?? '—' }}</template>
        </el-table-column>

        <el-table-column label="订单金额" width="130" align="right">
          <template #default="{ row }">
            <span class="amount">{{ formatMoney(row.totalAmount) }}</span>
          </template>
        </el-table-column>

        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusTagType(row.status)">{{ orderStatusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column label="商品明细" min-width="260">
          <template #default="{ row }">
            <template v-if="row.items && row.items.length">
              <el-tooltip
                v-for="(item, idx) in row.items"
                :key="idx"
                placement="top"
                :content="`${item.productName}｜单价 ${formatMoney(item.productPrice)} × ${item.quantity} = ${formatMoney(item.totalPrice)}`"
              >
                <el-tag size="small" class="item-tag" type="info">
                  {{ item.productName }} ×{{ item.quantity }}
                </el-tag>
              </el-tooltip>
            </template>
            <span v-else class="muted">—</span>
          </template>
        </el-table-column>

        <el-table-column label="创建时间" width="170" align="center">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>

        <el-table-column label="操作" width="230" fixed="right" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" link :icon="View" @click="handleDetail(row)">详情</el-button>

            <!-- 待支付：可支付 / 可取消（后端只允许 status=0 操作） -->
            <template v-if="row.status === ORDER_STATUS.UNPAID">
              <el-button size="small" type="success" link :icon="Wallet" @click="openPayDialog(row)">支付</el-button>
              <el-button size="small" type="danger" link :icon="CircleClose" @click="handleCancel(row)">取消</el-button>
            </template>

            <!-- 已支付：可发货 -->
            <el-button
              v-else-if="row.status === ORDER_STATUS.PAID"
              size="small"
              type="warning"
              link
              :icon="Van"
              @click="handleShip(row)"
            >发货</el-button>

            <!-- 已发货：可确认完成 -->
            <el-button
              v-else-if="row.status === ORDER_STATUS.SHIPPED"
              size="small"
              type="success"
              link
              :icon="CircleCheck"
              @click="handleFinish(row)"
            >完成</el-button>

            <el-dropdown trigger="click" @command="(cmd) => handleCommand(cmd, row)">
              <el-button size="small" type="info" link class="more-btn">
                更多<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="remark" :icon="EditPen">修改备注</el-dropdown-item>
                  <el-dropdown-item command="pay" :disabled="row.status !== ORDER_STATUS.UNPAID" :icon="Wallet">
                    支付订单
                  </el-dropdown-item>
                  <el-dropdown-item
                    command="cancel"
                    :disabled="row.status !== ORDER_STATUS.UNPAID"
                    :icon="CircleClose"
                  >取消订单</el-dropdown-item>
                  <el-dropdown-item
                    command="ship"
                    :disabled="row.status !== ORDER_STATUS.PAID"
                    :icon="Van"
                  >订单发货</el-dropdown-item>
                  <el-dropdown-item
                    command="finish"
                    :disabled="row.status !== ORDER_STATUS.SHIPPED"
                    :icon="CircleCheck"
                  >确认完成</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <!-- ==================== 分页 ==================== -->
      <el-pagination
        class="pagination"
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :page-sizes="[5, 10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="fetchList"
      />
    </el-card>

    <!-- ==================== 新建订单弹窗（下单选品） ==================== -->
    <el-dialog v-model="createVisible" title="新建订单（下单扣库存）" width="820px" top="6vh" destroy-on-close>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="api-tip"
        title="只可选择「已上架」商品；用户ID 由后端写死为 1，前端无需传。提交后立即扣减库存。"
      />

      <div class="item-list">
        <div v-for="(row, index) in orderForm.items" :key="index" class="item-row">
          <span class="row-index">{{ index + 1 }}</span>

          <el-select
            v-model="row.productId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="搜索并选择商品（名称）"
            :remote-method="loadProducts"
            :loading="productLoading"
            class="product-select"
            @change="(val) => onProductChange(row, val)"
          >
            <el-option
              v-for="p in mergedOptions"
              :key="p.id"
              :label="`${p.name}（库存 ${p.stock ?? 0}）`"
              :value="p.id"
              :disabled="(p.stock ?? 0) <= 0"
            />
          </el-select>

          <el-input-number
            v-model="row.quantity"
            :min="1"
            :max="row.product ? Math.max(1, row.product.stock ?? 1) : 9999"
            controls-position="right"
            class="qty-input"
          />

          <span class="subtotal">
            {{ row.product ? formatMoney(Number(row.product.price) * row.quantity) : '—' }}
          </span>

          <el-button type="danger" :icon="Delete" circle plain @click="removeItem(index)" />
        </div>
      </div>

      <div class="item-actions">
        <el-button type="primary" plain :icon="Plus" @click="addItem">添加商品</el-button>
        <span class="total-preview">
          合计：<b>{{ formatMoney(totalAmount) }}</b>
        </span>
      </div>

      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmitOrder">提交订单</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 支付弹窗 ==================== -->
    <el-dialog v-model="payVisible" title="订单支付" width="440px" destroy-on-close>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="订单号">{{ payForm.orderSn }}</el-descriptions-item>
        <el-descriptions-item label="应付金额">{{ formatMoney(payForm.totalAmount) }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px" class="pay-form">
        <el-form-item label="支付方式">
          <el-radio-group v-model="payForm.payType">
            <el-radio v-for="i in PAY_TYPE_OPTIONS" :key="i.value" :value="i.value">{{ i.label }}</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="payVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handlePayConfirm">确认支付</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 备注弹窗 ==================== -->
    <el-dialog v-model="remarkVisible" title="修改订单备注" width="480px" destroy-on-close>
      <el-form label-width="70px">
        <el-form-item label="订单号">
          <span>{{ remarkForm.orderSn }}</span>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="remarkForm.remark" type="textarea" :rows="3" maxlength="200" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="remarkVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleRemarkConfirm">保存</el-button>
      </template>
    </el-dialog>

    <!-- ==================== 订单详情抽屉 ==================== -->
    <el-drawer v-model="detailVisible" title="订单详情" size="680px" destroy-on-close>
      <div v-loading="detailLoading" class="detail-body">
        <template v-if="detail">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单ID">{{ detail.id }}</el-descriptions-item>
            <el-descriptions-item label="订单状态">
              <el-tag :type="orderStatusTagType(detail.status)">{{ orderStatusLabel(detail.status) }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="订单号" :span="2">{{ detail.orderSn }}</el-descriptions-item>
            <el-descriptions-item label="用户ID">{{ detail.userId ?? '—' }}</el-descriptions-item>
            <el-descriptions-item label="订单金额">
              <span class="amount">{{ formatMoney(detail.totalAmount) }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="创建时间" :span="2">{{ formatDateTime(detail.createTime) }}</el-descriptions-item>
          </el-descriptions>

          <div class="detail-block">
            <div class="block-title">商品明细（下单时的快照）</div>
            <el-table :data="detail.items || []" border size="small">
              <el-table-column prop="productId" label="商品ID" width="90" align="center" />
              <el-table-column prop="productName" label="商品名称" min-width="180" show-overflow-tooltip />
              <el-table-column label="单价" width="110" align="right">
                <template #default="{ row }">{{ formatMoney(row.productPrice) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="70" align="center" />
              <el-table-column label="小计" width="120" align="right">
                <template #default="{ row }">
                  <span class="amount">{{ formatMoney(row.totalPrice) }}</span>
                </template>
              </el-table-column>
            </el-table>
            <div class="detail-total">
              合计：<b class="amount">{{ formatMoney(detail.totalAmount) }}</b>
            </div>
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowDown,
  CircleCheck,
  CircleClose,
  Delete,
  EditPen,
  Plus,
  Refresh,
  Search,
  Van,
  View,
  Wallet
} from '@element-plus/icons-vue'
import {
  cancelOrder,
  createOrder,
  finishOrder,
  getOrderById,
  getOrderList,
  payOrder,
  shipOrder,
  updateOrder
} from '@/api/order'
import { getProductList } from '@/api/product'
import {
  ORDER_STATUS,
  PAY_TYPE_OPTIONS,
  formatDateTime,
  formatMoney,
  normalizePageResult,
  orderStatusLabel,
  orderStatusTagType
} from '@/utils/constants'

/* ==================== 列表 & 查询 ==================== */
const loading = ref(false)
const list = ref([])
const total = ref(0)
const approximate = ref(false)

const query = reactive({
  orderSn: '',
  pageNum: 1,
  pageSize: 10
})

async function fetchList() {
  loading.value = true
  try {
    const params = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.orderSn) params.orderSn = query.orderSn.trim()
    const data = await getOrderList(params)
    const page = normalizePageResult(data, { pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = page.list
    total.value = page.total
    approximate.value = page.approximate
  } catch (e) {
    list.value = []
    total.value = 0
    approximate.value = false
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  query.pageNum = 1
  fetchList()
}

function handleReset() {
  query.orderSn = ''
  query.pageNum = 1
  query.pageSize = 10
  fetchList()
}

function handleSizeChange() {
  query.pageNum = 1
  fetchList()
}

/* ==================== 支付 ==================== */
const payVisible = ref(false)
const submitting = ref(false)
const payForm = reactive({ id: null, orderSn: '', totalAmount: 0, payType: 1 })

function openPayDialog(row) {
  Object.assign(payForm, { id: row.id, orderSn: row.orderSn, totalAmount: row.totalAmount, payType: 1 })
  payVisible.value = true
}

async function handlePayConfirm() {
  submitting.value = true
  try {
    await payOrder(payForm.id, payForm.payType)
    ElMessage.success('支付成功')
    payVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

/* ==================== 取消 ==================== */
async function handleCancel(row) {
  let reason = ''
  try {
    const { value } = await ElMessageBox.prompt(
      `请填写取消订单「${row.orderSn}」的原因（后端必填，取消后会自动回补库存）`,
      '取消订单',
      {
        confirmButtonText: '确认取消订单',
        cancelButtonText: '再想想',
        inputType: 'textarea',
        inputPlaceholder: '例如：用户主动取消、库存异常、收货信息有误',
        inputValidator: (val) => (val && val.trim() ? true : '取消原因不能为空'),
        type: 'warning'
      }
    )
    reason = value.trim()
  } catch {
    return
  }
  await cancelOrder(row.id, reason)
  ElMessage.success('订单已取消，库存已回补')
  fetchList()
}

/* ==================== 发货 / 完成 ==================== */
async function handleShip(row) {
  try {
    await ElMessageBox.confirm(`确认对订单「${row.orderSn}」执行发货吗？`, '发货确认', { type: 'warning' })
  } catch {
    return
  }
  await shipOrder(row.id)
  ElMessage.success('发货成功')
  fetchList()
}

async function handleFinish(row) {
  try {
    await ElMessageBox.confirm(`确认订单「${row.orderSn}」已完成吗？`, '完成确认', { type: 'warning' })
  } catch {
    return
  }
  await finishOrder(row.id)
  ElMessage.success('订单已完成')
  fetchList()
}

/* ==================== 备注 ==================== */
const remarkVisible = ref(false)
const remarkForm = reactive({ id: null, orderSn: '', remark: '' })

function openRemarkDialog(row) {
  Object.assign(remarkForm, { id: row.id, orderSn: row.orderSn, remark: '' })
  remarkVisible.value = true
}

async function handleRemarkConfirm() {
  submitting.value = true
  try {
    await updateOrder({ id: remarkForm.id, remark: remarkForm.remark })
    ElMessage.success('备注已更新')
    remarkVisible.value = false
    fetchList()
  } finally {
    submitting.value = false
  }
}

function handleCommand(command, row) {
  if (command === 'remark') return openRemarkDialog(row)
  if (command === 'pay') return openPayDialog(row)
  if (command === 'cancel') return handleCancel(row)
  if (command === 'ship') return handleShip(row)
  if (command === 'finish') return handleFinish(row)
}

/* ==================== 新建订单 ==================== */
const createVisible = ref(false)
const productLoading = ref(false)
const productOptions = ref([])

const orderForm = reactive({ items: [] })

const mergedOptions = computed(() => {
  const map = new Map()
  productOptions.value.forEach((p) => map.set(p.id, p))
  // 已选中但不在当前搜索结果里的商品也要能正常回显
  orderForm.items.forEach((row) => {
    if (row.product && !map.has(row.product.id)) map.set(row.product.id, row.product)
  })
  return Array.from(map.values())
})

const totalAmount = computed(() =>
  orderForm.items.reduce((sum, row) => {
    if (!row.product) return sum
    return sum + Number(row.product.price) * Number(row.quantity || 0)
  }, 0)
)

function newItem() {
  return { productId: null, product: null, quantity: 1 }
}

/** 远程搜索：只查「已上架」商品 */
async function loadProducts(keyword = '') {
  productLoading.value = true
  try {
    const params = { publishStatus: 1, pageNum: 1, pageSize: 20 }
    if (keyword) params.keyword = keyword
    const data = await getProductList(params)
    productOptions.value = normalizePageResult(data, { pageNum: 1, pageSize: 20 }).list
  } catch (e) {
    productOptions.value = []
  } finally {
    productLoading.value = false
  }
}

function onProductChange(row, productId) {
  const found = mergedOptions.value.find((p) => p.id === productId) || null
  row.product = found
  if (found) {
    const stock = Number(found.stock ?? 0)
    if (stock > 0 && row.quantity > stock) row.quantity = stock
  }
}

function addItem() {
  orderForm.items.push(newItem())
}

function removeItem(index) {
  if (orderForm.items.length === 1) {
    ElMessage.warning('至少保留一件商品')
    return
  }
  orderForm.items.splice(index, 1)
}

function handleCreateOrder() {
  orderForm.items = [newItem()]
  createVisible.value = true
  loadProducts('')
}

async function handleSubmitOrder() {
  const rows = orderForm.items
  if (!rows.length) {
    ElMessage.warning('请至少添加一件商品')
    return
  }
  const invalid = rows.find((r) => !r.productId)
  if (invalid) {
    ElMessage.warning('请为每一行选择商品')
    return
  }
  const noStock = rows.find((r) => r.product && Number(r.product.stock ?? 0) < Number(r.quantity || 0))
  if (noStock) {
    ElMessage.warning(`商品「${noStock.product.name}」库存不足（剩余 ${noStock.product.stock}）`)
    return
  }

  submitting.value = true
  try {
    const vo = await createOrder({
      items: rows.map((r) => ({ productId: r.productId, quantity: Number(r.quantity) }))
    })
    createVisible.value = false
    ElMessage.success(`下单成功${vo?.orderSn ? `，订单号：${vo.orderSn}` : ''}`)
    fetchList()
    // 下单后询问是否立即支付（后端只允许待支付订单支付）
    if (vo?.id) {
      try {
        await ElMessageBox.confirm(
          `订单 ${vo.orderSn} 金额 ${formatMoney(vo.totalAmount)}，是否立即支付？`,
          '支付确认',
          { confirmButtonText: '立即支付', cancelButtonText: '稍后支付', type: 'info' }
        )
        openPayDialog(vo)
      } catch {
        // 选择稍后支付
      }
    }
  } finally {
    submitting.value = false
  }
}

/* ==================== 详情抽屉 ==================== */
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)

async function handleDetail(row) {
  detailVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getOrderById(row.id)
  } finally {
    detailLoading.value = false
  }
}

const route = useRoute()

onMounted(() => {
  // 支持从「数据概览」点击订单号跳转过来时自动带上订单号查询
  if (route.query.orderSn) query.orderSn = String(route.query.orderSn)
  fetchList()
})
</script>

<style scoped>
.search-card {
  margin-bottom: 16px;
}

.api-tip {
  margin-top: 8px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}

.item-tag {
  margin: 0 4px 4px 0;
}

.muted {
  color: #c0c4cc;
}

.amount {
  color: #f56c6c;
  font-weight: 600;
}

.more-btn {
  margin-left: 8px;
}

/* 新建订单 */
.item-list {
  margin-top: 14px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.row-index {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 24px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #409eff;
  color: #fff;
  font-size: 12px;
}

.product-select {
  flex: 1 1 auto;
}

.qty-input {
  width: 140px;
  flex: 0 0 140px;
}

.subtotal {
  flex: 0 0 110px;
  text-align: right;
  color: #f56c6c;
  font-weight: 600;
}

.item-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 6px;
}

.total-preview {
  color: #606266;
}

.total-preview b {
  color: #f56c6c;
  font-size: 18px;
}

.pay-form {
  margin-top: 16px;
}

/* 详情 */
.detail-body {
  min-height: 200px;
}

.detail-block {
  margin-top: 18px;
}

.block-title {
  font-weight: 600;
  margin-bottom: 10px;
  color: #303133;
}

.detail-total {
  margin-top: 12px;
  text-align: right;
  color: #606266;
}

.detail-total b {
  font-size: 16px;
}
</style>