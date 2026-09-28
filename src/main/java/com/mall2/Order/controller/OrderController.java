package com.mall2.Order.controller;

import com.mall2.Order.dto.CreateOrderDTO;
import com.mall2.Order.dto.OrderVO;
import com.mall2.Order.model.Order;
import com.mall2.Order.service.OrderService;
import com.mall2.common.CommonResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Slf4j
@RestController
@Validated // 新增！开启 @RequestParam / @PathVariable 单个基础参数校验
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // ==================== 下单接口 ====================

    /**
     * 创建订单（下单扣库存）
     * POST /api/order/create
     * 参数校验：通过 @Valid 触发 CreateOrderDTO 中的校验规则
     */
    @PostMapping("/create")
    public CommonResult<OrderVO> createOrder(@Valid @RequestBody CreateOrderDTO dto) {
        OrderVO order = orderService.createOrder(dto);
        return CommonResult.success(order, "下单成功");
    }

    // ==================== 查询接口 ====================

    @GetMapping("/detail/id/{id}")
    public CommonResult<OrderVO> getOrderById(@PathVariable Long id) {
        log.info("根据ID查询订单：{}", id);
        OrderVO order = orderService.getOrderById(id);
        return CommonResult.success(order);
    }

    @GetMapping("/detail/sn/{orderSn}")
    public CommonResult<OrderVO> getOrderBySn(@PathVariable String orderSn) {
        log.info("根据订单号查询订单：{}", orderSn);
        OrderVO order = orderService.getOrderBySn(orderSn);
        return CommonResult.success(order);
    }

    /**
     * 更新订单信息（通用更新）
     * 校验：只要求 ID 不为空，其他字段按需更新
     */
    @PostMapping("/update")
    public CommonResult<Void> updateOrder(@Valid @RequestBody Order order) {
        log.info("更新订单：{}", order);
        orderService.updateOrder(order);
        return CommonResult.success(null, "订单更新成功");
    }

    // ==================== 状态流转接口 ====================

    @PostMapping("/pay/{id}")
    public CommonResult<Void> paySuccess(@PathVariable Long id,
                                         @RequestParam(defaultValue = "1") Integer payType) {
        log.info("支付回调，订单ID：{}，支付方式：{}", id, payType);
        orderService.paySuccess(id, payType);
        return CommonResult.success(null, "支付成功");
    }

    /**
     * 取消订单
     * 校验：reason 必填（@NotBlank 由 Spring 自动校验，无需手动抛异常）
     */
    @PostMapping("/cancel/{id}")
    public CommonResult<Void> cancelOrder(@PathVariable Long id,
                                          @RequestParam @NotBlank(message = "取消原因不能为空") String reason) {
        log.info("取消订单，订单ID：{}，原因：{}", id, reason);
        orderService.cancelOrder(id, reason);
        return CommonResult.success(null, "订单已取消");
    }
}