package com.score.entity.bo;

import jakarta.validation.constraints.Pattern;
import lombok.Data;


/**
 * @author imyiwen
 * @data 2026/4/21 17:12
 */
@Data
public class StudentQueryScoreBo {
    private String studentName;

    @Pattern(regexp = "^\\d{17}[\\dX]$", message = "身份证号格式不正确，应为18位")
    private String idCard;

    private String className;
    private String examName;

    /**
     * 恢复驼峰命名
     */
    private String userName;
}
