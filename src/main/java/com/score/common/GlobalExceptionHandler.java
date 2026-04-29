package com.score.common;

import lombok.extern.slf4j.Slf4j;
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
    @ExceptionHandler(Exception.class)
    public ResultVo<?> handleException(Exception e) {
        log.error("系统异常",e);
        return ResultVo.error(e.getMessage());
    }

    //特别处理Sa-Token登陆异常
    @ExceptionHandler(NotLoginException.class)
    public ResultVo<?> handleException(NotLoginException e) {
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
