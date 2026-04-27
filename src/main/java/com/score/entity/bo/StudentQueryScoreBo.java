package com.score.entity.bo;

import lombok.Data;

/**
 * @author imyiwen
 * @data 2026/4/21 17:12
 */
@Data
public class StudentQueryScoreBo {
    private String studentName;
    private String idCard;
    private String className;

    private Integer pageNum = 1;
    private Integer pageSize = 10;

    /**
     * 恢复驼峰命名
     */
    private String userName;
}
