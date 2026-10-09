package com.mall2.Order.mapper;

import com.mall2.Order.model.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface OrderItemMapper {

    int insert(OrderItem orderItem);

    /**
     * 批量插入订单明细
     */
    int batchInsert(@Param("list") List<OrderItem> list);
//根据总订单的id查询所有子订单的列表
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}