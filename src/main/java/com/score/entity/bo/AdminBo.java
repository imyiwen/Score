package com.score.entity.bo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @author imyiwen
 * @data 2026/4/21 17:11
 */
@Data
public class AdminBo {
    @JsonProperty("userName")
    private String userName;
    
    private String password;
    
    @JsonProperty("className")
    private String className;

    private String captchaId;
}
