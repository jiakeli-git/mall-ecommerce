# Mall E-Commerce · 后端服务

Spring Boot 3.x + MyBatis + MySQL 电商平台后端核心服务，包含商品管理和订单管理两大核心模块。

## 技术栈

| 组件 | 版本/说明 |
|---|---|
| Spring Boot | 4.0.8 |
| MyBatis | 4.0.1 (mybatis-spring-boot-starter) |
| MySQL Driver | mysql-connector-j (runtime) |
| Lombok | optional |
| PageHelper | 1.4.7 (分页插件) |
| Hutool | 5.8.40 (工具类) |
| JSR 303 Validation | spring-boot-starter-validation |
| Java | 17 |

## 快速开始

```bash
cd backend
mvn spring-boot:run
```

## 数据库准备

```sql
CREATE DATABASE mall2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 导入 SQL 建表脚本（product / t_order / t_order_item）
```

数据库连接配置位于 `src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/mall2?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: root
    password: 123456
```

## 模块结构

```
backend/
├── src/main/java/com/mall2/
│   ├── Product/                    # 商品模块
│   │   ├── controller/ProductController.java
│   │   ├── service/impl/ProductServiceImpl.java
│   │   ├── mapper/ProductMapper.java
│   │   ├── model/Product.java
│   │   └── dto/ProductQueryParam.java
│   ├── Order/                      # 订单模块
│   │   ├── controller/OrderController.java
│   │   ├── service/Impl/OrderServiceImpl.java
│   │   ├── mapper/OrderMapper.java / OrderItemMapper.java
│   │   ├── model/Order.java / OrderItem.java
│   │   └── dto/CreateOrderDTO.java / OrderVO.java
│   └── common/                     # 通用组件
│       ├── CommonResult.java       # 统一返回格式 {code, message, data}
│       ├── CommonPage.java         # 分页封装（适配 PageHelper PageInfo）
│       └── Exceptions/             # 自定义异常及全局处理器
└── src/main/resources/
    ├── application.yml             # 服务配置（端口 8080, MyBatis, PageHelper）
    └── com/mall2/*/mapper/*.xml     # MyBatis Mapper XML（与包路径一致）
```

## RESTful API 总览

### 商品管理 ` /api/product `

| 方法 | 路径 | 说明 | 参数 |
|---|---|---|---|
| GET | `/list` | 分页查询商品列表 | keyword(名称模糊)、productSn(精确)、categoryId、brandId、publishStatus、verifyStatus、pageNum、pageSize |
| GET | `/detail/{id}` | 获取商品详情 | path id |
| POST | `/create` | 新增商品 | body: Product (name/price/stock 必填) |
| POST | `/update` | 更新商品 | body: Product (id/name/price/stock 必填) |
| POST | `/delete/{id}` | 逻辑删除商品 | path id |
| POST | `/publish/{id}` | 上架商品 | 要求：审核通过+当前下架+未删除 |
| POST | `/unpublish/{id}` | 下架商品 | 要求：当前为上架 |
| POST | `/verify/pass/{id}` | 审核通过 | 要求：当前待审核 |
| POST | `/verify/reject/{id}` | 审核驳回 | 要求：当前待审核+reason非空 |

### 订单管理 `/api/order`

| 方法 | 路径 | 说明 | 参数 |
|---|---|---|---|
| POST | `/create` | 创建订单（下单扣库存） | body: `{ items: [{ productId, quantity }] }` |
| GET | `/list` | 订单分页列表 | orderSn(精确)、pageNum、pageSize |
| GET | `/detail/id/{id}` | 订单详情（按 ID） | path id |
| GET | `/detail/sn/{sn}` | 订单详情（按订单号） | path orderSn |
| POST | `/update` | 通用更新订单 | body: Order (仅 id 必填) — 用于改备注、发货(2)、完成(3) |
| POST | `/pay/{id}` | 标记支付成功 | path id; payType(1支付宝/2微信/3银联)，仅 status=0 可操作 |
| POST | `/cancel/{id}` | 取消订单 | path id; reason(必填)，仅 status=0 可操作；取消后自动回补库存 |

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

- code = 200：成功
- code = 400：参数校验失败
- code = 500：服务器错误

### 商品字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| id | Long | 索引 |
| productSn | String | 货号（自动生成 P+日期+4位随机数） |
| name | String | 商品名称（必填） |
| subTitle | String | 副标题 / 卖点 |
| categoryId | Long | 分类 ID |
| brandId | Long | 品牌 ID |
| price | BigDecimal | 销售价格（必填） |
| originalPrice | BigDecimal | 原价 |
| stock | Integer | 库存（必填，乐观锁扣减/回补） |
| unit | String | 计量单位（件/个/台） |
| pic | String | 主图 URL |
| albumPics | String | 轮播图（逗号分隔） |
| description | String | 商品简介 |
| detailHtml | String | HTML 富文本详情 |
| sale | Integer | 销量 |
| sort | Integer | 排序值 |
| publishStatus | Integer | 0-下架 1-上架 |
| verifyStatus | Integer | 0-待审核 1-通过 2-驳回 |
| rejectReason | String | 驳回原因 |
| deleteStatus | Integer | 0-未删除 1-已删除 |

### 订单状态码

| 值 | 含义 |
|---|---|
| 0 | 待支付 |
| 1 | 已支付 |
| 2 | 已发货 |
| 3 | 已完成 |
| 4 | 已取消 |

## 核心设计要点

- **乐观锁扣减库存**：下单时用 version 版本号防止超卖；取消订单时自动回补库存
- **订单快照**：下单时将商品名称、价格等信息写入 `t_order_item`，以后商品改名改价不影响历史订单
- **审核流程**：新商品默认 `verifyStatus=0`（待审核），管理员审核后（1 通过 / 2 驳回）才能上架
- **通用更新接口**：订单的发货、完成等非标准状态流转通过 `POST /api/order/update` 直接修改 `status` 字段实现