package com.score.entity.bo;

import lombok.Data;

/**
 * @author imyiwen
 * @data 2026/4/21 17:12
 */
@Data
public class StudentQueryScoreBo {
    private String studentName;
    /**
     * 身份证
     */
    private String idCard;
    /**
     * 班级名（多租户过滤用）
     */
    private String className;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;
    /**
     * 每页数量
     */
    private Integer pageSize = 10;
}
