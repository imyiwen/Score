package com.score.Listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.score.entity.Student;
import com.score.entity.vo.StudentImportExcelVo;
import com.score.mapper.StudentMapper;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 增强版学生导入监听器：处理合并单元格和脏数据
 */
public class StudentImportListener extends AnalysisEventListener<StudentImportExcelVo> {
    private StudentMapper studentMapper;
    private String className;
    private List<Student> cacheDataList = new ArrayList<>();
    
    // 用于记忆上一行的非空数据，解决合并单元格问题
    private String lastIdCard = "";

    public StudentImportListener(StudentMapper studentMapper, String className) {
        this.studentMapper = studentMapper;
        this.className = className;
    }

    @Override
    public void invoke(StudentImportExcelVo data, AnalysisContext context) {
        // 1. 过滤脏数据：如果没有姓名，则认为是空行或无效行
        if (data == null || !StringUtils.hasText(data.getStudentName())) {
            return;
        }

        // 2. 处理合并单元格逻辑：如果身份证号为空，尝试使用上一行的记录
        if (!StringUtils.hasText(data.getIdCard())) {
            data.setIdCard(lastIdCard);
        } else {
            lastIdCard = data.getIdCard();
        }

        // 3. 封装实体
        Student student = new Student();
        student.setStudentName(data.getStudentName());
        student.setClassName(className);
        student.setIdCard(data.getIdCard());
        student.setDelFlag("0");
        student.setCreateTime(new Date());
        cacheDataList.add(student);

        // 4. 批量插入，提升性能
        if (cacheDataList.size() >= 50) {
            saveData();
            cacheDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    private void saveData() {
        if (!cacheDataList.isEmpty()) {
            for (Student student : cacheDataList) {
                studentMapper.insert(student);
            }
        }
    }
}
