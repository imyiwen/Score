package com.score.entity;


import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * @author imyiwen
 * @data 2026/4/21 16:47
 */

@Data
public class ScoreImportExcelVo {

    @ExcelProperty(index=0,value="学号")
    private String studentNo;

    @ExcelProperty(index=1,value="姓名")
    private String studentName;

    @ExcelProperty(index=2,value="科目")
    private String subject;

    @ExcelProperty(index=3,value="分数")
    private BigDecimal score;
}
