package com.mall2.Order.model;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class OrderItem {
    private Long id;//索引
    private Long orderId;//外键链接总订单
    private Long productId;//商品id
    private String productName;// 订单名称快照,查询后复制不在变化
    private BigDecimal productPrice; // 订单单价快照，查询后复制不在变化
    private Integer quantity;//订单数量
    private BigDecimal totalPrice;//该订单的总价（单价*数量）

}
