package com.score.controller;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cloud.tianai.captcha.application.vo.ImageCaptchaVO;
import cloud.tianai.captcha.common.constant.CaptchaTypeConstant;
import cloud.tianai.captcha.common.response.ApiResponse;
import cloud.tianai.captcha.validator.common.model.dto.ImageCaptchaTrack;
import jakarta.servlet.http.HttpSession;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

/**
 * 验证码控制器
 *
 * @author imyiwen
 * @data 2026/6/30
 */

@RestController
@RequestMapping("/captcha")
@Slf4j
public class CaptchaController {
    @Autowired
    private ImageCaptchaApplication application;

    @Autowired
    private HttpSession httpSession;

    /**
     * 生成验证码
     * @return 验证码数据
     */
    @PostMapping("/genCaptcha")
    public ApiResponse<ImageCaptchaVO> genCaptcha() {
        // 1.生成验证码(该数据返回给前端用于展示验证码数据)
        // 参数1为具体的验证码类型， 默认支持 SLIDER、ROTATE、WORD_IMAGE_CLICK、CONCAT 等验证码类型，详见： `CaptchaTypeConstant`类
        return application.generateCaptcha(CaptchaTypeConstant.SLIDER);
    }

    /**
     * 校验验证码
     * @param data 验证码数据
     * @return 校验结果
     */
    @PostMapping("/check")
    @ResponseBody
    public ApiResponse<?> checkCaptcha(@RequestBody Data data) {
        ApiResponse<?> response = application.matching(data.getId(), data.getData());
        if (response.isSuccess()) {
            // 验证码校验成功，将 captchaId 标记存入 Session

            httpSession.setAttribute("captcha_verified_" + data.getId(), true);
            log.info("验证码校验成功，captchaId: {}", data.getId());
            return ApiResponse.ofSuccess(Collections.singletonMap("id", data.getId()));
        }
        log.warn("验证码校验失败，captchaId: {}", data.getId());
        return response;
    }

    @lombok.Data
    public static class Data {
        // 验证码id,前端回传的验证码ID
        private String id;
        // 验证码数据,前端回传的验证码轨迹数据
        private ImageCaptchaTrack data;
    }
}