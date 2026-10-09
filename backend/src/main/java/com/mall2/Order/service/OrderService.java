package com.mall2.Order.service;

import com.github.pagehelper.Page;
import com.mall2.Order.dto.CreateOrderDTO;
import com.mall2.Order.dto.OrderVO;
import com.mall2.Order.model.Order;
import jdk.jfr.DataAmount;
import lombok.AllArgsConstructor;

public interface OrderService {
    //创建订单，并且返回ordervo
    OrderVO createOrder(CreateOrderDTO dto);
    /**
     * 根据订单ID查询订单详情
     * GET /api/order/detail/id/1
     */
    OrderVO getOrderById(Long id);
    /**
     * 根据订单号查询订单详情
     * GET /api/order/detail/sn/ORD202608232145120123
     */
    OrderVO getOrderBySn(String orderSn);
    /**
     * 更新订单信息（通用更新，只传需要修改的字段）
     * POST /api/order/update
     *
     * 请求体示例（只传要改的字段）：
     * {
     *   "id": 1,
     *   "remark": "用户备注：加急发货"
     * }
     */
    void updateOrder(Order order);
    //    状态流转--支付订单
    void paySuccess(Long id, Integer payType);
    //    状态流转--支付失败
    void cancelOrder(Long id, String reason);


    Page<OrderVO> listOrder(String orderSn, Integer pageNum, Integer pageSize);
}
