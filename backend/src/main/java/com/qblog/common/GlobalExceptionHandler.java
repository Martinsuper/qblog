package com.qblog.common;

import com.qblog.common.exception.BusinessException;
import com.qblog.common.exception.ResourceNotFoundException;
import com.qblog.common.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 同时设置真实的 HTTP 状态码，便于监控、网关与前端按状态码识别错误
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e, HttpServletResponse response) {
        log.warn("业务异常：{}", e.getMessage());
        response.setStatus(toHttpStatus(e.getCode()));
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理资源不存在异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Result<Void> handleResourceNotFoundException(ResourceNotFoundException e, HttpServletResponse response) {
        log.warn("资源不存在：{}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return Result.error(404, e.getMessage());
    }

    /**
     * 处理未授权异常
     */
    @ExceptionHandler(UnauthorizedException.class)
    public Result<Void> handleUnauthorizedException(UnauthorizedException e, HttpServletResponse response) {
        log.warn("未授权访问：{}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return Result.error(401, e.getMessage());
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidationException(MethodArgumentNotValidException e, HttpServletResponse response) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败：{}", message);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return Result.error(400, "参数校验失败：" + message);
    }

    /**
     * 处理绑定异常
     */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBindException(BindException e, HttpServletResponse response) {
        String message = e.getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数绑定失败：{}", message);
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return Result.error(400, "参数错误：" + message);
    }

    /**
     * 处理IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgumentException(IllegalArgumentException e, HttpServletResponse response) {
        log.warn("参数错误：{}", e.getMessage());
        response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        return Result.error(400, e.getMessage());
    }

    /**
     * 处理数据访问异常
     */
    @ExceptionHandler(DataAccessException.class)
    public Result<Void> handleDataAccessException(DataAccessException e, HttpServletResponse response) {
        log.error("数据访问异常：{}", e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Result.error(500, "数据库操作失败，请稍后重试");
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public Result<Void> handleUnreadable(org.springframework.http.converter.HttpMessageNotReadableException e, HttpServletResponse response) {
        response.setStatus(400);
        return Result.error(400, "无效的JSON请求");
    }

    @ExceptionHandler(org.springframework.web.HttpMediaTypeNotSupportedException.class)
    public Result<Void> handleMediaType(org.springframework.web.HttpMediaTypeNotSupportedException e, HttpServletResponse response) {
        response.setStatus(415);
        return Result.error(415, "Content-Type must be application/json");
    }

    @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class)
    public Result<Void> handleMethod(org.springframework.web.HttpRequestMethodNotSupportedException e, HttpServletResponse response) {
        response.setStatus(405);
        return Result.error(405, "Method not supported");
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public Result<Void> handleAccessDenied(org.springframework.security.access.AccessDeniedException e, HttpServletResponse response) {
        response.setStatus(403);
        return Result.error(403, "无权限访问该资源");
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public Result<Void> handleAuthentication(org.springframework.security.core.AuthenticationException e, HttpServletResponse response) {
        response.setStatus(401);
        return Result.error(401, "未授权，请先登录");
    }

    /**
     * 处理运行时异常（未被其他处理器捕获的）
     */
    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e, HttpServletResponse response) {
        log.error("运行时异常：{}", e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Result.error(500, "服务器内部错误");
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e, HttpServletResponse response) {
        log.error("系统异常：{}", e.getMessage(), e);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        return Result.error(500, "服务器内部错误");
    }

    /**
     * 业务码映射为合法的 HTTP 状态码（4xx/5xx），非法值按 400 处理
     */
    private int toHttpStatus(int code) {
        return (code >= 400 && code <= 599) ? code : 400;
    }
}
