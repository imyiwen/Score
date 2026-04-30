package com.score.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author imyiwen
 * @data 2026/4/22 11:10
 */
@Data
public class StudentImportExcelVo {

    @ExcelProperty(value = "姓名")
    private String studentName;

    @ExcelProperty(value = "身份证")
    private String idCard;
    @ExcelProperty(value = "班级")
    private String className;


}
