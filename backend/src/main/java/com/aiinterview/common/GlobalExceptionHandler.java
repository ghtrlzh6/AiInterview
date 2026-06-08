package com.aiinterview.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusiness(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldError)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("errors", errors);
        return Result.fail(400, "参数校验失败", data);
    }

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    public Result<Void> handleAuth(AuthenticationException e) {
        return Result.fail(401, e.getMessage() != null ? e.getMessage() : "认证失败");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied() {
        return Result.fail(403, "无权限访问");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleBadRequest(HttpMessageNotReadableException e) {
        return Result.fail(400, "请求体格式错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return Result.fail(400, "Upload file cannot exceed 10MB");
    }

    @ExceptionHandler(MultipartException.class)
    public Result<Void> handleMultipart(MultipartException e) {
        return Result.fail(400, "Invalid upload request, please choose a PDF file again");
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleGeneral(Exception e) {
        log.error("Unhandled exception", e);
        return Result.fail(500, "服务器内部错误");
    }

    private Map<String, String> toFieldError(FieldError fe) {
        Map<String, String> m = new HashMap<>();
        m.put("field", fe.getField());
        m.put("message", fe.getDefaultMessage());
        return m;
    }
}
