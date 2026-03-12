package com.qblog.common.exception;

/**
 * 资源不存在异常
 */
public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(404, message);
    }

    public ResourceNotFoundException(String resourceType, Long id) {
        super(404, String.format("%s (ID: %d) 不存在", resourceType, id));
    }
}
