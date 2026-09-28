package com.mall2.common.Exceptions;

import lombok.Getter;

/**
 * 业务异常
 * 使用场景：库存不足、订单状态不允许操作、商品不存在等
 * 触发时机：Service 层业务逻辑校验失败时抛出
 * 返回给用户：具体的业务提示信息（如："库存不足，剩余10件"）
 */
@Getter
public class BusinessException extends RuntimeException {

    private final Integer code;

    /**
     * 构造方法：只传错误信息，默认错误码为 500
     */
    public BusinessException(String message) {
        super(message);
        this.code = 500;
    }

    /**
     * 构造方法：自定义错误码和信息
     * @param code 业务错误码（如：1001-库存不足，1002-状态错误）
     * @param message 错误描述
     */
    public BusinessException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}