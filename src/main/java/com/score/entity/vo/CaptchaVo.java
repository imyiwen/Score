package com.score.entity.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author imyiwen
 * @data 2026/4/29 9:39
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CaptchaVo {
    //标识本次验证
    private String captchaId;
    //背景图Base64
    private String backgroundImage;
    //滑块图Base64
    private String sliderImage;
}
