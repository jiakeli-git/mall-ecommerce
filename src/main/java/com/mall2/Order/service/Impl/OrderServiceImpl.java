package com.mall2.Order.service.Impl;

import com.mall2.Order.dto.CreateOrderDTO;
import com.mall2.Order.dto.OrderVO;
import com.mall2.Order.mapper.OrderItemMapper;
import com.mall2.Order.mapper.OrderMapper;
import com.mall2.Order.model.Order;
import com.mall2.Order.model.OrderItem;
import com.mall2.Order.service.OrderService;
import com.mall2.Product.mapper.ProductMapper;
import com.mall2.Product.model.Product;
import com.mall2.common.Exceptions.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;

    // 构造方法注入，不需要@Autowired(Spring4.3+单构造器可省略)
    public OrderServiceImpl(OrderMapper orderMapper,
                            OrderItemMapper orderItemMapper,
                            ProductMapper productMapper) {
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
    }

    //生成订单并返回vo
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO createOrder(CreateOrderDTO dto) {
        // ========== 1. 参数校验 ==========
        List<CreateOrderDTO.OrderItemDTO> items = dto.getItems();
        if (items == null || items.isEmpty()) {
            throw new BusinessException("下单商品不能为空");
        }
        // ========== 2. 用户ID（暂写死，后期从Token解析） ==========
        Long userId = 1L;
        // ========== 3. 生成订单号 ==========
        String orderSn = generateOrderSn();
        // ========== 4. 计算总金额 & 组装快照列表 ==========
        BigDecimal totalAmount = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CreateOrderDTO.OrderItemDTO item : items) {
            // 4.1 查询商品（获取真实价格和名称）
            Product product = productMapper.selectById(item.getProductId());
            log.info("product version={}",product.getVersion());
            if (product == null) {
                throw new BusinessException("商品ID " + item.getProductId() + " 不存在");
            }
            // 4.2 检查上下架状态（已下架不能下单）
            if (product.getPublishStatus() == 0) {
                throw new BusinessException("商品 " + product.getName() + " 已下架，无法购买");
            }
            // 4.3 乐观锁扣减库存
            int rows = productMapper.decreaseStock(
                    item.getProductId(),
                    item.getQuantity(),
                    product.getVersion()
            );
            if (rows == 0) {
                // 影响行数为0：版本号已被改，说明库存被其他人抢先扣了
                throw new BusinessException("商品 " + product.getName() + " 库存不足或已被抢光，请重试");
            }
            // 4.4 计算小计
            BigDecimal subtotal = product.getPrice().multiply(new BigDecimal(item.getQuantity()));
            // 4.5 构建订单明细快照
            OrderItem orderItem = new OrderItem();
//           快照含义： 商品以后改名字、改价格，老订单记录不变。
//如果不存快照，以后商品价格修改，查历史订单价格就错乱。
//订单项把下单那一刻的商品名称、价格保存下来。
            orderItem.setProductId(product.getId());
            orderItem.setProductName(product.getName());      // 快照
            orderItem.setProductPrice(product.getPrice());    // 快照
            orderItem.setQuantity(item.getQuantity());
            orderItem.setTotalPrice(subtotal);
            orderItems.add(orderItem);
            totalAmount = totalAmount.add(subtotal);
        }
        // ========== 5. 插入订单主表 ==========
        Order order = new Order();
        order.setOrderSn(orderSn);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(0); // 待支付
        order.setRemark("用户下单");
        orderMapper.insert(order);
        // ========== 6. 插入订单明细表 ==========
        Long orderId = order.getId();
        for (OrderItem item : orderItems) {
            item.setOrderId(orderId);
        }
        orderItemMapper.batchInsert(orderItems);

        log.info("订单创建成功，订单号：{}，用户ID：{}，总金额：{}", orderSn, userId, totalAmount);
        // ========== 7. 组装返回 VO ==========
        return buildOrderVO(order, orderItems);

    }


    /**
     * 根据订单ID查询订单详情
     * GET /api/order/detail/id/1
     */
    @Override
    public OrderVO getOrderById(Long id) {
        if (id == null) {
            throw new BusinessException("订单ID不能为空");
        }
        Order order = orderMapper.selectById(id);
        if (order == null) {
            throw new BusinessException("订单不存在，ID：" + id);
        }
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(id);
        return buildOrderVO(order, orderItems);
    }
    /**
     * 根据订单号查询订单详情
     * GET /api/order/detail/sn/ORD202608232145120123
     */
    @Override
    public OrderVO getOrderBySn(String orderSn) {
        if (!StringUtils.hasText(orderSn)) {
            throw new BusinessException("订单号不能为空");
        }
        Order order = orderMapper.selectByOrderSn(orderSn);
        if (order == null) {
            throw new BusinessException("订单不存在，订单号：" + orderSn);
        }
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(order.getId());
        return buildOrderVO(order, orderItems);
    }

    @Override
    public void updateOrder(Order order) {
        if (order == null || order.getId() == null) {
            throw new BusinessException("订单信息不完整，缺少ID");
        }
        // 校验订单是否存在
        Order existing = orderMapper.selectById(order.getId());
        if (existing == null) {
            throw new BusinessException("订单不存在，ID：" + order.getId());
        }
        int rows = orderMapper.updateById(order);
        if (rows == 0) {
            throw new BusinessException("订单更新失败，请重试");
        }
        log.info("订单更新成功，订单ID：{}", order.getId());
    }




    //    状态流转--支付订单
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void paySuccess(Long orderId, Integer payType) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }
        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在，ID：" + orderId);
        }

        // 2. 状态校验：只有待支付的订单才能支付
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许支付，当前状态：" + getStatusDesc(order.getStatus()));
        }

        // 3. 更新订单状态
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(1);                          // 已支付
        update.setPayType(payType);                   // 支付方式
        update.setPayTime(LocalDateTime.now());       // 支付时间
        orderMapper.updateById(update);

        log.info("订单支付成功，订单ID：{}，支付方式：{}", orderId, payType);
    }
    //    状态流转--取消订单
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId, String reason) {
        if (orderId == null) {
            throw new BusinessException("订单ID不能为空");
        }

        // 1. 查询订单
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在，ID：" + orderId);
        }

        // 2. 状态校验：只有待支付的订单才能取消
        if (order.getStatus() != 0) {
            throw new BusinessException("订单状态不允许取消，当前状态：" + getStatusDesc(order.getStatus()));
        }

        // 3. 更新订单状态
        Order update = new Order();
        update.setId(orderId);
        update.setStatus(4);                          // 已取消
        update.setCancelTime(LocalDateTime.now());    // 取消时间
        update.setCancelReason(reason);               // 取消原因
        orderMapper.updateById(update);

        // 4. 回补库存
        List<OrderItem> orderItems = orderItemMapper.selectByOrderId(orderId);
        for (OrderItem item : orderItems) {
            // 重要：重新查询商品拿最新version，OrderItem快照里没有version！
            Product product = productMapper.selectById(item.getProductId());
            if(product == null){
                log.warn("回补库存，商品已不存在 productId={}",item.getProductId());
                continue;
            }
            //乐观锁回补库存
            int rows = productMapper.increaseStock(item.getProductId(), item.getQuantity(), product.getVersion());
            if(rows == 0){
                //版本冲突，回补失败，实际项目可以告警、记录异常日志，人工处理
                log.error("库存回补失败，版本冲突 productId={}, quantity={}",item.getProductId(),item.getQuantity());
                throw new BusinessException("订单取消库存回补失败，请重试");
            }
            log.info("回补库存成功：商品ID {}，数量 {}", item.getProductId(), item.getQuantity());
        }

        log.info("订单取消成功，订单ID：{}，原因：{}", orderId, reason);
    }

    // ========== 工具方法 ==========

    /**
     * 生成订单号：ORD + 年月日时分秒 + 4位随机数
     */
    private String generateOrderSn() {
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String random = String.format("%04d", new Random().nextInt(10000));
        return "ORD" + date + random;
    }

    /**
     * 构建返回 VO
     */
    private OrderVO buildOrderVO(Order order, List<OrderItem> items) {
        OrderVO vo = new OrderVO();
        vo.setId(order.getId());
        vo.setOrderSn(order.getOrderSn());
        vo.setUserId(order.getUserId());
        vo.setTotalAmount(order.getTotalAmount());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(getStatusDesc(order.getStatus()));
        vo.setCreateTime(order.getCreateTime());

        List<OrderVO.OrderItemVO> itemVOS = new ArrayList<>();
        for (OrderItem item : items) {
            OrderVO.OrderItemVO itemVO = new OrderVO.OrderItemVO();
            itemVO.setProductId(item.getProductId());
            itemVO.setProductName(item.getProductName());
            itemVO.setProductPrice(item.getProductPrice());
            itemVO.setQuantity(item.getQuantity());
            itemVO.setTotalPrice(item.getTotalPrice());
            itemVOS.add(itemVO);
        }
        vo.setItems(itemVOS);
        return vo;
    }
    /**
     * 状态中文描述
     */
    private String getStatusDesc(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待支付";
            case 1 -> "已支付";
            case 2 -> "已发货";
            case 3 -> "已完成";
            case 4 -> "已取消";
            default -> "未知";
        };
    }

}
