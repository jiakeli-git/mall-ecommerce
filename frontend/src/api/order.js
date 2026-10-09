import request from '@/utils/request'

/**
 * 订单相关接口
 * 严格对齐后端：com.mall2.Order.controller.OrderController（@RequestMapping("/api/order")）
 *
 * 后端实际提供的接口只有下面这些，前端不再调用不存在的接口：
 *   POST /api/order/create            下单（扣库存）
 *   GET  /api/order/list              分页列表（按订单号）
 *   GET  /api/order/detail/id/{id}    详情（按 ID）
 *   GET  /api/order/detail/sn/{sn}    详情（按订单号）
 *   POST /api/order/update            通用更新（只有 id 必传）
 *   POST /api/order/pay/{id}          支付
 *   POST /api/order/cancel/{id}       取消（reason 必填，后端自动回补库存）
 */

/**
 * 订单分页列表
 * GET /api/order/list?orderSn=&pageNum=1&pageSize=10
 * 响应：CommonResult<Page<OrderVO>>
 * ⚠️ PageHelper 的 Page 继承 ArrayList，Jackson 会序列化成 JSON 数组（total 丢失），
 *    请配合 utils/constants.js 的 normalizePageResult 使用。
 * 后端 orderSn 为精确匹配（=），不是模糊匹配。
 */
export function getOrderList(params) {
  return request({
    url: '/order/list',
    method: 'get',
    params
  })
}

/**
 * 创建订单（下单扣库存）
 * POST /api/order/create
 * body: { items: [{ productId, quantity }] }   // userId 由后端写死，前端不传
 * 返回：OrderVO（含 orderSn / totalAmount / items）
 */
export function createOrder(data) {
  return request({
    url: '/order/create',
    method: 'post',
    data
  })
}

/** 订单详情（按 ID）：GET /api/order/detail/id/{id} */
export function getOrderById(id) {
  return request({
    url: `/order/detail/id/${id}`,
    method: 'get'
  })
}

/** 订单详情（按订单号）：GET /api/order/detail/sn/{orderSn} */
export function getOrderBySn(orderSn) {
  return request({
    url: `/order/detail/sn/${orderSn}`,
    method: 'get'
  })
}

/**
 * 通用更新订单
 * POST /api/order/update    body: Order（只要求 id 非空，其余字段按需更新）
 * 用于：修改备注、发货（status=2）、确认完成（status=3）
 */
export function updateOrder(data) {
  return request({
    url: '/order/update',
    method: 'post',
    data
  })
}

/**
 * 支付订单
 * POST /api/order/pay/{id}?payType=1
 * 业务约束（后端校验）：只有 status=0（待支付）才能支付
 */
export function payOrder(id, payType = 1) {
  return request({
    url: `/order/pay/${id}`,
    method: 'post',
    params: { payType }
  })
}

/**
 * 取消订单
 * POST /api/order/cancel/{id}?reason=xxx
 * 业务约束（后端校验）：reason 必填；只有 status=0 才能取消；取消后自动回补库存
 */
export function cancelOrder(id, reason) {
  return request({
    url: `/order/cancel/${id}`,
    method: 'post',
    params: { reason }
  })
}

/* ==================== 状态流转封装（复用后端通用更新接口） ====================
 * 后端只有“支付 / 取消”两个专门的状态流转接口，
 * 发货(1→2)、确认完成(2→3) 通过通用的 /api/order/update 修改 status 实现。
 */

/** 发货：status 1 → 2 */
export function shipOrder(id) {
  return updateOrder({ id, status: 2 })
}

/** 确认完成：status 2 → 3 */
export function finishOrder(id) {
  return updateOrder({ id, status: 3 })
}