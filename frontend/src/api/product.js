import request from '@/utils/request'

/**
 * 商品相关接口
 * 严格对齐后端：com.mall2.Product.controller.ProductController（@RequestMapping("/api/product")）
 *
 *   GET  /api/product/list           分页查询（keyword 只对商品名称 name 做模糊匹配）
 *   GET  /api/product/detail/{id}    商品详情
 *   POST /api/product/create         新增（后端自动生成货号，默认 下架/待审核）
 *   POST /api/product/update         更新（name、price、stock 必传）
 *   POST /api/product/delete/{id}    删除（后端为逻辑删除 deleteStatus=1）
 *   POST /api/product/publish/{id}        上架（要求：审核通过 + 当前下架 + 未删除）
 *   POST /api/product/unpublish/{id}      下架（要求：当前为上架）
 *   POST /api/product/verify/pass/{id}    审核通过（要求：当前待审核）
 *   POST /api/product/verify/reject/{id}  审核驳回（要求：当前待审核 + reason 非空）
 */

/**
 * 分页查询商品列表
 * @param {Object} params
 *   keyword        商品名称模糊查询
 *   productSn      货号（精确匹配）
 *   categoryId     分类 ID
 *   brandId        品牌 ID
 *   publishStatus  0-下架 1-上架
 *   verifyStatus   0-待审核 1-通过 2-驳回
 *   pageNum / pageSize
 * 返回：CommonPage { pageNum, pageSize, total, totalPage, list }
 */
export function getProductList(params) {
  return request({
    url: '/product/list',
    method: 'get',
    params
  })
}

/** 商品详情 */
export function getProductDetail(id) {
  return request({
    url: `/product/detail/${id}`,
    method: 'get'
  })
}

/**
 * 新增商品
 * body: Product（name / price / stock 必填；货号与状态由后端生成）
 */
export function createProduct(data) {
  return request({
    url: '/product/create',
    method: 'post',
    data
  })
}

/**
 * 更新商品（需传 id，且 name / price / stock 必填 —— 后端 @Valid 走默认分组）
 */
export function updateProduct(data) {
  return request({
    url: '/product/update',
    method: 'post',
    data
  })
}

/** 删除商品（后端逻辑删除） */
export function deleteProduct(id) {
  return request({
    url: `/product/delete/${id}`,
    method: 'post'
  })
}

/** 上架（需先审核通过） */
export function publishProduct(id) {
  return request({
    url: `/product/publish/${id}`,
    method: 'post'
  })
}

/** 下架 */
export function unpublishProduct(id) {
  return request({
    url: `/product/unpublish/${id}`,
    method: 'post'
  })
}

/** 审核通过 */
export function verifyPassProduct(id) {
  return request({
    url: `/product/verify/pass/${id}`,
    method: 'post'
  })
}

/**
 * 审核驳回
 * @param {number} id
 * @param {string} reason 驳回原因（后端必填，为空会报错）
 */
export function verifyRejectProduct(id, reason) {
  return request({
    url: `/product/verify/reject/${id}`,
    method: 'post',
    params: { reason }
  })
}