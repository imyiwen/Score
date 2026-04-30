package com.score.controller;

import com.score.common.CaptchaUtils;
import com.score.common.ResultVo;
import com.score.entity.vo.CaptchaVo;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * @author imyiwen
 * @data 2026/4/29 10:12
 */
@RestController
@RequestMapping("/captcha")
@Slf4j
public class CaptchaController {
    @GetMapping("/get")
    public ResultVo<?>getCaptcha(HttpSession session) throws Exception {
        CaptchaUtils.CaptchaResult result = new CaptchaUtils.CaptchaResult();
        CaptchaUtils.createCaptcha(result);
        String captchaId = UUID.randomUUID().toString();
        session.setAttribute("captcha_x_"+captchaId,result.getX());
        return ResultVo.success(new CaptchaVo(captchaId,result.getBackBase64(),result.getSliderBase64()));
    }
    @PostMapping("/check")
    public ResultVo<?> checkCaptcha(@RequestBody Map<String,Object>data,HttpSession session){
        String captchaId=(String) data.get("captchaId");
        Integer moveX=(Integer) data.get("moveX");
        Integer realX=(Integer) session.getAttribute("captcha_x_"+captchaId);
        if(realX==null){
            return ResultVo.error("验证码已过期");
        }
        if(Math.abs(realX-moveX)<=5){
            session.setAttribute("captcha_verified_"+captchaId,true);
            return ResultVo.success(captchaId);
        }
        return ResultVo.error("验证码校验失败");
    }
}
