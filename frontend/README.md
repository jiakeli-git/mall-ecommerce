# mall-vue3 · 电商后台管理系统

Vue 3 + Vite + Element Plus 的电商管理后台，接口严格对齐后端项目 `re-mall (mall2)` 的
`ProductController`（`/api/product`）与 `OrderController`（`/api/order`）。

## 页面与功能

### 数据概览（/dashboard）
- 统计卡片：商品总数 / 已上架 / 已下架 / 待审核 / 审核驳回 / 订单总数（估算）
  （复用 `GET /api/product/list` 的 `total`，`pageSize=1` 开销极小）
- 最新订单表格：可点击订单号跳转到订单管理页并自动带入查询条件
- 待处理事项：待审核 / 驳回 / 下架 / 最新订单中待支付数量
- 快捷入口：直达商品管理、订单管理

### 商品管理（/product）
- 多条件搜索：商品名称（模糊）、货号（精确）、上架状态、审核状态 + 分页
- 列表展示：主图（可预览）、货号、名称/副标题、售价/原价、库存（低库存变色）、销量、上架状态、审核状态（驳回原因悬浮提示）、创建时间
- 新增商品：完整表单（名称、副标题、价格、原价、库存、分类、品牌、单位、销量、排序、主图、轮播图、简介、富文本详情）；
  新商品由后端默认置为「下架 / 待审核」，货号自动生成
- 编辑商品：先调 `GET /api/product/detail/{id}` 拉取最新数据再回填表单
- 商品详情抽屉：全字段 + 图片预览 + 富文本详情渲染
- 业务操作（按钮禁用逻辑与后端校验完全一致，禁用时悬浮提示原因）：
  - 上架：仅「审核通过 + 当前下架 + 未删除」可操作
  - 下架：仅「当前上架」可操作
  - 审核通过 / 审核驳回：仅「待审核」可操作，驳回必须填写原因
  - 删除：后端为逻辑删除，二次确认

### 订单管理（/order）
- 按订单号查询（后端为精确匹配）+ 分页
- 列表展示：订单号、用户、金额、状态标签、商品明细（悬浮显示快照单价/数量/小计）、创建时间
- 新建订单（下单选品）：
  - 远程搜索选择「已上架」商品，显示库存并限制购买数量上限
  - 多行商品、实时小计与合计、库存不足前端预校验
  - 下单成功后可选「立即支付」
- 状态流转（按当前状态显示可用操作）：
  - 待支付 → 支付（选择支付宝/微信/银联）或 取消（必填原因，后端自动回补库存）
  - 已支付 → 发货（通过通用更新接口改 status=2）
  - 已发货 → 确认完成（status=3）
  - 任意状态 → 修改备注、查看详情抽屉（订单信息 + 商品快照明细表）
- 分页兼容：订单列表接口返回 PageHelper 的 `Page`（被 Jackson 序列化为数组、不含 total），
  前端用 `normalizePageResult` 估算总条数并给出提示

## 后端接口映射

| 前端调用 | 后端接口 |
| --- | --- |
| getProductList | GET /api/product/list |
| getProductDetail | GET /api/product/detail/{id} |
| createProduct / updateProduct | POST /api/product/create、/update |
| deleteProduct | POST /api/product/delete/{id}（逻辑删除） |
| publishProduct / unpublishProduct | POST /api/product/publish/{id}、/unpublish/{id} |
| verifyPassProduct / verifyRejectProduct | POST /api/product/verify/pass/{id}、/verify/reject/{id}?reason= |
| getOrderList | GET /api/order/list?orderSn=&pageNum=&pageSize= |
| createOrder | POST /api/order/create（items: [{productId, quantity}]） |
| getOrderById / getOrderBySn | GET /api/order/detail/id/{id}、/detail/sn/{sn} |
| updateOrder（含 shipOrder/finishOrder 封装） | POST /api/order/update |
| payOrder | POST /api/order/pay/{id}?payType= |
| cancelOrder | POST /api/order/cancel/{id}?reason= |

## 目录结构

```
src/
├── api/
│   ├── product.js        # 商品接口封装
│   └── order.js          # 订单接口封装（含发货/完成状态流转）
├── utils/
│   ├── request.js        # axios 封装（统一解包 CommonResult）
│   └── constants.js      # 状态字典、格式化、分页归一化
├── views/
│   ├── dashboard/        # 数据概览
│   ├── product/          # 商品管理
│   └── order/            # 订单管理
├── layout/               # 侧边栏 + 顶栏布局
├── router/               # 路由（含标题同步、404 兜底）
└── main.js
```

## 启动

```sh
npm install
npm run dev     # 开发（vite 已配置 /api → http://localhost:8080 代理）
npm run build   # 生产构建
```

> 注意：后端需先启动（默认 8080 端口）；跨域由 vite dev 代理解决，无需后端配置 CORS。

## 截图展示

<!-- 可执行 `npm run preview` + Edge --headless --screenshot 自行生成 -->
| 页面 | 预览 |
|---|---|
| 数据概览 | ![Dashboard](../docs/screenshots/dashboard.png) |
| 商品管理 | ![Product List](../docs/screenshots/product-list.png) |
| 订单管理 | ![Order List](../docs/screenshots/order-list.png) |

> 💡 若本地缺少截图文件，可参照 [根 README](../README.md) 中的截图生成指南。