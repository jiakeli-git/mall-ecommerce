/**
 * 与后端 re-mall(mall2) 接口对齐的字典与工具方法
 * ------------------------------------------------------------
 * 后端关键事实（来自实际代码，不是猜的）：
 * 1. 商品 Product：
 *    publishStatus  0-下架 1-上架
 *    verifyStatus   0-待审核 1-审核通过 2-审核驳回
 *    deleteStatus   0-未删除 1-已删除（后端 delete 是逻辑删除）
 * 2. 订单 Order：status 0-待支付 1-已支付 2-已发货 3-已完成 4-已取消
 *    （OrderServiceImpl.getStatusDesc 的口径）
 *    ⚠️ OrderMapper.xml 里列表 SQL 的 status_desc 只覆盖 0/1/2 且有偏差，
 *       所以前端一律按 status 数字自行翻译，不直接使用 statusDesc。
 * 3. 商品列表返回 CommonPage：{ pageNum, pageSize, total, totalPage, list }
 *    订单列表返回 PageHelper 的 Page —— 它继承 ArrayList，
 *    Jackson 会序列化成纯 JSON 数组（total 丢失），需要前端兜底估算。
 */

/* ==================== 订单状态 ==================== */
export const ORDER_STATUS = {
  UNPAID: 0,
  PAID: 1,
  SHIPPED: 2,
  FINISHED: 3,
  CANCELED: 4
}

const ORDER_STATUS_MAP = {
  0: { label: '待支付', tag: 'warning' },
  1: { label: '已支付', tag: 'primary' },
  2: { label: '已发货', tag: 'success' },
  3: { label: '已完成', tag: 'success' },
  4: { label: '已取消', tag: 'info' }
}

/** 订单状态中文 */
export function orderStatusLabel(status) {
  return ORDER_STATUS_MAP[status]?.label || '未知状态'
}

/** 订单状态对应的 el-tag 类型 */
export function orderStatusTagType(status) {
  return ORDER_STATUS_MAP[status]?.tag || 'info'
}

/* ==================== 支付方式 ==================== */
export const PAY_TYPE_OPTIONS = [
  { value: 1, label: '支付宝' },
  { value: 2, label: '微信支付' },
  { value: 3, label: '银联' }
]

export function payTypeLabel(payType) {
  return PAY_TYPE_OPTIONS.find((i) => i.value === payType)?.label || `其他(${payType})`
}

/* ==================== 商品审计 / 上架状态 ==================== */
export const VERIFY_STATUS_OPTIONS = [
  { value: 0, label: '待审核', tag: 'warning' },
  { value: 1, label: '审核通过', tag: 'success' },
  { value: 2, label: '审核驳回', tag: 'danger' }
]

export const PUBLISH_STATUS_OPTIONS = [
  { value: 1, label: '已上架', tag: 'success' },
  { value: 0, label: '已下架', tag: 'info' }
]

export function verifyStatusLabel(status) {
  return VERIFY_STATUS_OPTIONS.find((i) => i.value === status)?.label || '未知'
}

export function verifyStatusTagType(status) {
  return VERIFY_STATUS_OPTIONS.find((i) => i.value === status)?.tag || 'info'
}

export function publishStatusLabel(status) {
  return status === 1 ? '已上架' : '已下架'
}

export function publishStatusTagType(status) {
  return status === 1 ? 'success' : 'info'
}

/* ==================== 格式化 ==================== */
function pad(n) {
  return String(n).padStart(2, '0')
}

/** 金额格式化：1234.5 → ¥1,234.50 */
export function formatMoney(value, symbol = '¥') {
  if (value === null || value === undefined || value === '') return '—'
  const num = Number(value)
  if (Number.isNaN(num)) return '—'
  return symbol + num.toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ',')
}

/**
 * 时间格式化
 * 后端 LocalDateTime 正常序列化为 "2026-08-23T21:45:12"，
 * 这里额外兼容 Jackson 时间戳数组 [2026,8,23,21,45,12] 的极端情况。
 */
export function formatDateTime(value) {
  if (value === null || value === undefined || value === '') return '—'
  if (Array.isArray(value)) {
    const [y, m = 1, d = 1, h = 0, mi = 0, s = 0] = value
    return `${y}-${pad(m)}-${pad(d)} ${pad(h)}:${pad(mi)}:${pad(s)}`
  }
  const str = String(value).replace('T', ' ')
  return str.length > 19 ? str.slice(0, 19) : str
}

/**
 * 分页结果归一化，兼容后端两种返回形态：
 *  - CommonPage（商品列表）：{ list, total, ... }
 *  - PageHelper 的 Page 被序列化成数组（订单列表）：[ ... ]
 * 数组形态下 total 拿不到，这里给出“估算总条数 + approximate 标记”。
 */
export function normalizePageResult(data, { pageNum = 1, pageSize = 10 } = {}) {
  // 形态一：CommonPage
  if (data && !Array.isArray(data) && Array.isArray(data.list)) {
    return {
      list: data.list,
      total: Number(data.total ?? data.list.length),
      totalPage: data.totalPage ?? null,
      approximate: false
    }
  }
  // 形态二：Page 序列化后的数组
  if (Array.isArray(data)) {
    const size = Number(pageSize) || 10
    const current = Number(pageNum) || 1
    const loaded = data.length
    const approximate = loaded >= size
    return {
      list: data,
      // 满页时说明后面可能还有数据，多给 1 条让“下一页”按钮可用
      total: (current - 1) * size + loaded + (approximate ? 1 : 0),
      totalPage: null,
      approximate
    }
  }
  return { list: [], total: 0, totalPage: null, approximate: false }
}