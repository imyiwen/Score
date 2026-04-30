package com.score.entity.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 动态成绩导入模型
 */
@Data
public class ScoreImportExcelVo {
    @ExcelProperty("学号")
    private String studentNo;

    @ExcelProperty("姓名")
    private String studentName;

    // 使用 Map 存储动态列，key 为科目名，value 为分数
    // 注意：EasyExcel 需要配合自定义 Converter 或者配置才能实现动态列读取，
    // 这里采用 Map<String, Object> 结合 ExcelIgnoreUnannotated 等方式处理
    private Map<String, BigDecimal> subjectScores;
}
