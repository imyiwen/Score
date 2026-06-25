package com.score.entity.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


/**
 * @author imyiwen
 * @data 2026/4/21 17:11
 */
@Data
public class AdminBo {
    @NotBlank(message = "用户名不能为空")
    @JsonProperty("userName")
    private String userName;

    @NotBlank(message = "密码不能为空")
    private String password;

    @JsonProperty("className")
    private String className;

    private String captchaId;
}
