package com.score.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import cn.dev33.satoken.exception.NotLoginException;

/**
 * @author imyiwen
 * @data 2026/4/21 17:25
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    // 处理 @Valid 校验失败，返回具体的校验错误信息
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResultVo<?> handleValidException(MethodArgumentNotValidException e) {
        log.warn("参数校验失败", e);
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("参数校验失败");
        return ResultVo.error(msg);
    }

    @ExceptionHandler(Exception.class)
    public ResultVo<?> handleException(Exception e) {
        log.error("系统异常",e);
        return ResultVo.error("系统繁忙，请稍后再试");
    }

    //处理数字格式异常
    @ExceptionHandler(NumberFormatException.class)
    public ResultVo<?>handleNumberFormatException(NumberFormatException e) {
        log.error("数据格式错误",e);
        return ResultVo.error("Excel数据格式错误，请检查分数是否为有效数字");
    }

    //特别处理Sa-Token登陆异常
    @ExceptionHandler(NotLoginException.class)
    public ResultVo<?> handleNotLoginException(NotLoginException e) {
        String msg="请重新登录";
        if(e.getType().equals(NotLoginException.BE_REPLACED)){
            msg="您的账户已在其他设备登录！";
        }else if(e.getType().equals(NotLoginException.INVALID_TOKEN)){
            msg="登陆已失效，请重新登录";
        }
        log.warn("登陆校验失败：{}",msg);
        return ResultVo.error(401,msg);
    }
}
