package com.qblog.knowledge;

import com.qblog.common.Result;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Keep invalid knowledge requests at HTTP 400 despite the legacy catch-all advice. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = KnowledgeController.class)
public class KnowledgeRequestAdvice {
    @ExceptionHandler({HttpMessageNotReadableException.class, MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<Result<Void>> invalidRequest(Exception ex) {
        return ResponseEntity.badRequest().body(Result.error(400, "参数格式错误或缺少必需参数（删除笔记需要version）"));
    }
}
