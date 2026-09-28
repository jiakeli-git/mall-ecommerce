package com.mall2.Order.mapper;

import com.mall2.Order.model.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface OrderMapper {
    void insert(Order order);//插入一个总订单

    Order selectById(@Param("id") Long id);//根据id查询一个总订单，并返回id

    Order selectByOrderSn(@Param("orderSn") String orderSn);//根据总订单单号查询

    int updateById(Order order);//修改订单内容

}
