package com.score.Listener;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.event.AnalysisEventListener;
import com.score.entity.Student;
import com.score.entity.vo.StudentImportExcelVo;
import com.score.mapper.StudentMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * @author imyiwen
 * @data 2026/4/22 11:01
 */
public class StudentImportListener extends AnalysisEventListener<StudentImportExcelVo> {
    private StudentMapper studentMapper;
    private String className;
    private List<Student> cacheDataList = new ArrayList<>();

    public StudentImportListener(StudentMapper studentMapper,String className) {
        this.studentMapper = studentMapper;
        this.className = className;
    }

    @Override
    public void invoke(StudentImportExcelVo data, AnalysisContext context) {
        Student student = new Student();
        student.setStudentName(data.getStudentName());
        student.setClassName(className);
        student.setIdCard(data.getIdCard());
        student.setDelFlag("0");
        cacheDataList.add(student);

        //达到一定数量 批量存入数据库
        if(cacheDataList.size()>=20){
            saveData();
            cacheDataList.clear();
        }
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext context) {
        saveData();
    }

    private void saveData() {
        if(!cacheDataList.isEmpty()){
            for(Student student : cacheDataList){
                studentMapper.insert(student);
            }
        }
    }
}
