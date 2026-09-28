package com.mall2.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommonResult<T> {
    private long code;
    private String message;
    private T data;
    //成功
    public static <T> CommonResult<T> success(T data) {
        return new CommonResult<>(200, "操作成功", data);
    }

    public static <T> CommonResult<T> success(T data, String message) {
        return new CommonResult<>(200, message, data);
    }
    //失败
    public static <T> CommonResult<T> failed(String message) {
        return new CommonResult<>(500, message, null);
    }

    public static <T> CommonResult<T> failed() {
        return new CommonResult<>(500, "操作失败", null);
    }
    //验者失败
    public static <T> CommonResult<T> validateFailed(String message) {
        return new CommonResult<>(400, message, null);
    }
}
