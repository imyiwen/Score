package com.score.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * @author imyiwen
 * @data 2026/4/22 11:10
 */
@Data
public class StudentImportExcelVo {

    @ExcelProperty(index = 0,value = "姓名")
    private String studentName;

    @ExcelProperty(index = 1,value = "身份证号")
    private String idCard;

    private String className;


}
