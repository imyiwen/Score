package com.score.Listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.score.entity.Student;
import com.score.entity.vo.StudentImportExcelVo;
import com.score.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class StudentImportListener extends AnalysisEventListener<StudentImportExcelVo> {
    private final StudentMapper studentMapper;
    private final String className;
    private final List<Student> cacheDataList = new ArrayList<>();
    private static final int BATCH_SIZE = 50;

    public StudentImportListener(StudentMapper studentMapper, String className) {
        this.studentMapper = studentMapper;
        this.className = className;
    }

    @Override
    public void invoke(StudentImportExcelVo data, AnalysisContext context) {
        if (data == null || !StringUtils.hasText(data.getStudentName()) || !StringUtils.hasText(data.getIdCard())) {
            log.warn("跳过空行或缺失关键信息");
            return;
        }

        Student student = new Student();
        student.setStudentName(data.getStudentName().trim());
        student.setIdCard(data.getIdCard().trim());
        student.setClassName(className);
        student.setDelFlag("0");
        student.setCreateTime(new Date());
        cacheDataList.add(student);

        if (cacheDataList.size() >= BATCH_SIZE) {
            saveData();
            cacheDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    private void saveData() {
        if (cacheDataList.isEmpty()) return;
        for (Student student : cacheDataList) {
            // 检查是否已存在（根据身份证号）
            LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Student::getIdCard, student.getIdCard());
            Student existing = studentMapper.selectOne(queryWrapper);

            if (existing != null) {
                // 如果存在则更新（仅更新名字和班级，保持身份证唯一）
                existing.setStudentName(student.getStudentName());
                existing.setClassName(student.getClassName());
                studentMapper.updateById(existing);
                log.info("学生信息已更新: {}", student.getStudentName());
            } else {
                // 不存在则插入
                studentMapper.insert(student);
                log.info("学生信息已新增: {}", student.getStudentName());
            }
        }
    }
}
