package com.mall2.Order.dto;

import lombok.Data;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
public class CreateOrderDTO {

    /**
     * 用户ID：后期从 Token 解析，现在写死，前端不传！
     * 所以这里注释掉，不暴露给前端
     */
    // private Long userId;

    /**
     * 下单商品列表，不能为空
     * @Valid 表示对列表中的每个元素进行递归校验
     */
    @NotEmpty(message = "下单商品不能为空")
    @Valid
    private List<OrderItemDTO> items;

    @Data
    public static class OrderItemDTO {

        /**
         * 商品ID，不能为空
         */
        @NotNull(message = "商品ID不能为空")
        private Long productId;

        /**
         * 购买数量，必须 >= 1
         */
        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量必须大于0")
        private Integer quantity;
    }
}