package com.mall2.Order.dto;

import jdk.jfr.DataAmount;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Data
/**
 * 订单返回 VO（给前端展示）
 * 包含订单主信息 + 所有明细快照
 */
public class OrderVO {
    private Long id;//总订单的索引
    private String orderSn;//总订单的唯一单号
    private Long userId;//用户id
    private BigDecimal totalAmount;//订单总价
    private Integer status;//订单状态
    private String statusDesc;  // 状态中文描述
    private LocalDateTime createTime;//订单创建时间
    private List<OrderItemVO> items;//总订单里面的小定单列表

    @Data
    public static class OrderItemVO {//每一个小定单
        private Long productId;//每一个小定单的id
        private String productName;//每一个小定单的name(快照)
        private BigDecimal productPrice;//每一个小定单的单价(快照)
        private Integer quantity;//每一个小定单的数量
        private BigDecimal totalPrice;//每一个小定单的总价
    }
}
