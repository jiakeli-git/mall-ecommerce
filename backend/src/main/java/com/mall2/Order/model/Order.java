package com.mall2.Order.model;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Order {

    /**
     * 订单ID，更新时必传
     */
    @NotNull(message = "订单ID不能为空")
    private Long id;

    private String orderSn;
    private Long userId;
    private BigDecimal totalAmount;
    private Integer status;          // 0-待支付 1-已支付 2-已发货 3-已完成 4-已取消
    private LocalDateTime payTime;
    private Integer payType;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}