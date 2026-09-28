package com.mall2.common.Exceptions;

import lombok.Getter;

/**
 * 系统异常
 * 使用场景：数据库连接失败、空指针、IO异常等
 * 触发时机：底层系统故障，不是用户操作导致的
 * 返回给用户：友好的通用提示（"系统繁忙，请稍后重试"）
 *
 * 注意：这个异常通常不直接抛出，而是由 GlobalExceptionHandler 兜底时使用
 */
@Getter
public class SystemException extends RuntimeException {

    private final Integer code;

    public SystemException(String message) {
        super(message);
        this.code = 500;
    }

    public SystemException(Integer code, String message) {
        super(message);
        this.code = code;
    }
}