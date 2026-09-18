package com.ccb.queue.web;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public R conflict(IllegalStateException e) {
        return new R(409, e.getMessage(), null);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public R badRequest(IllegalArgumentException e) {
        return new R(400, e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public R other(Exception e) {
        return new R(HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "服务异常: " + e.getMessage(), null);
    }
}
