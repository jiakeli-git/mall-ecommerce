package com.mall2.common.Exceptions;

import com.mall2.common.CommonResult;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常拦截器
 *
 * 工作原理：
 * 1. 被 @RestControllerAdvice 标记的类，会扫描所有 Controller
 * 2. 当 Controller 中抛出异常时，Spring 会根据异常类型，匹配对应的 @ExceptionHandler 方法
 * 3. 匹配到的 Handler 方法执行，将其返回值作为 HTTP 响应返回给前端
 *
 * 处理流程：
 * Controller 抛异常 → Spring 拦截 → 匹配 @ExceptionHandler → 返回 CommonResult
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ==================== 1. 业务异常 ====================

    /**
     * 处理业务异常
     * 适用场景：库存不足、状态流转错误、数据不存在等
     * 返回状态码：500（业务错误）
     */
    @ExceptionHandler(BusinessException.class)
    public CommonResult<Void> handleBusinessException(BusinessException e) {
        // 记录警告日志（业务异常是预期内的，不需要 ERROR 级别）
        log.warn("业务异常：{}", e.getMessage());
        return CommonResult.failed(e.getMessage());
    }

    // ==================== 2. 参数异常（三种常见场景） ====================

    /**
     * 处理参数异常（手动抛出）
     * 适用场景：在 Controller 或 Service 中手动校验参数后抛出
     */
    @ExceptionHandler(ParamException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)  // 返回 HTTP 400
    public CommonResult<Void> handleParamException(ParamException e) {
        log.warn("参数异常：{}", e.getMessage());
        return CommonResult.validateFailed(e.getMessage());
    }

    /**
     * 处理 JSR380 参数校验异常（@RequestBody + @Valid）
     * 适用场景：CreateOrderDTO 中的 @NotNull、@Min 等校验不通过
     * 异常类型：MethodArgumentNotValidException
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 提取所有校验失败的字段信息，拼接成一条错误消息
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return CommonResult.validateFailed(message);
    }

    /**
     * 处理 JSR380 参数校验异常（@RequestParam + @Validated）
     * 适用场景：Controller 方法中的单个参数校验（如 @Min(1) Integer pageNum）
     * 异常类型：ConstraintViolationException
     */
    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleConstraintViolationException(ConstraintViolationException e) {
        String message = e.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        return CommonResult.validateFailed(message);
    }

    /**
     * 处理表单参数绑定异常（@ModelAttribute + @Valid / GET查询对象绑定）
     * 适用场景：GET 请求的 Query 参数绑定对象时校验失败
     * 异常类型：BindException
     */
    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public CommonResult<Void> handleBindException(BindException e) {
        // BindException 本身直接提供 getFieldErrors()，无需 getSuppressed
        String message = e.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining("; "));

        log.warn("参数绑定失败：{}", message);
        return CommonResult.validateFailed(message);
    }

    // ==================== 3. 系统异常（兜底） ====================

    /**
     * 处理所有未捕获的异常（兜底）
     * 适用场景：空指针、数据库连接失败、数组越界等非预期异常
     * 返回给用户：友好的通用提示（不暴露敏感信息！）
     *
     * 注意：这个方法会捕获所有没有被上面的 Handler 处理的异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public CommonResult<Void> handleException(Exception e) {
        // 记录错误日志（系统异常是 bug，需要 ERROR 级别报警，必须传异常对象e打印堆栈）
        log.error("系统异常", e);
        // 返回给用户的必须是通用提示，不能把堆栈信息返回给前端！
        return CommonResult.failed("系统繁忙，请稍后重试");
    }
}
