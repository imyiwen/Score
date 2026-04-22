package com.score.entity.vo;


import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author imyiwen
 * @data 2026/4/21 16:47
 */

@Data
public class ScoreImportExcelVo {

    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String studentName;

    @ExcelProperty("科目")
    private String subject;

    @ExcelProperty("分数")
    private BigDecimal score;
}
