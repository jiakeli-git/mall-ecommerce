package com.mall2.common.Exceptions;

import lombok.Getter;
/**
 * 参数异常
 * 使用场景：前端传递的请求参数不合法
 * 触发时机：Controller 层参数校验（JSR303）失败，或手动校验参数时
 * 返回给用户：具体的参数错误提示（如："商品ID不能为空"）
 */
@Getter
public class ParamException extends RuntimeException {

    private final Integer code;

    public ParamException(String message) {
        super(message);
        this.code = 400;  // HTTP 400 Bad Request
    }

    public ParamException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}