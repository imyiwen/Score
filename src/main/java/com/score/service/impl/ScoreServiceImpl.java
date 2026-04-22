package com.score.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.score.common.ResultVo;
import com.score.common.UserContext;
import com.score.entity.Admin;
import com.score.entity.Score;
import com.score.entity.Student;
import com.score.entity.bo.AdminBo;
import com.score.entity.bo.StudentQueryScoreBo;
import com.score.entity.vo.ScoreImportExcelVo;
import com.score.entity.vo.StudentImportExcelVo;
import com.score.mapper.AdminMapper;
import com.score.mapper.ScoreMapper;
import com.score.mapper.StudentMapper;
import com.score.service.IScoreService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @author imyiwen
 * @data 2026/4/22 9:45
 */

@Service
public class ScoreServiceImpl implements IScoreService {

    @Resource
    private AdminMapper adminMapper;

    @Resource
    private ScoreMapper scoreMapper;
    @Autowired
    private StudentMapper studentMapper;

    /**
     * 学生查询成绩
     * @param bo
     * @return
     */
    @InterceptorIgnore(tenantLine = "true")
    @Override
    public ResultVo<?> queryScore(StudentQueryScoreBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getStudentName())) {
            return ResultVo.error("查询失败：姓名不能为空");
        }
        LambdaQueryWrapper<Score> lqw=buildScoreQueryWrapper(bo,true);
        return ResultVo.success(scoreMapper.selectList(lqw));
    }

    /**
     * 教师查询成绩（分页）
     * @param bo
     * @return
     */
    @Override
    public ResultVo<?> checkList(StudentQueryScoreBo bo) {
        if (bo != null && StringUtils.hasText(bo.getClassName())) {
            UserContext.setClassName(bo.getClassName());
        }
        try {
            // 分页参数获取
            Page<Score> page = new Page<>(bo.getPageNum(), bo.getPageSize());
            LambdaQueryWrapper<Score> lqw=buildScoreQueryWrapper(bo,false);
            // selectPage 会自动处理总数和当前页数据
            return ResultVo.success(scoreMapper.selectPage(page, lqw));
        } finally {
            UserContext.remove();
        }
    }

    /**
     *教师登录
     */
    @Override
    public ResultVo<?> login(AdminBo bo){
        LambdaQueryWrapper<Admin> lqw =buildAdminQueryWrapper(bo);
        return ResultVo.success(adminMapper.selectOne(lqw));
    }

    /**
     * 导入学生信息
     */
    @Override
    public ResultVo<?> importStudent(MultipartFile file,String className){
        UserContext.setClassName(className);
        try{
            List<StudentImportExcelVo> list = EasyExcel.read(file.getInputStream())
                    .head(StudentImportExcelVo.class)
                    .sheet()
                    .doReadSync();

            List<Student> students =new ArrayList<>();
            for(StudentImportExcelVo vo:list){
                Student student =new Student();
                student.setStudentName(vo.getStudentName());
                student.setIdCard(vo.getIdCard());
                student.setClassName(className);
                student.setDelFlag("0");
                student.setCreateTime(new Date());
                students.add(student);
            }
            // 后续可优化为 saveBatch
            for(Student student:students){
                studentMapper.insert(student);
            }
            return ResultVo.success("导入成功"+students.size()+"条学生信息");
        }catch(IOException e){
            return ResultVo.error("解析Excel失败");
        }finally {
            UserContext.remove();
        }
    }

    /**
     * 导入成绩
     */
    @Override
    public ResultVo<?> importScores(MultipartFile file,String examName){
        try{
            List<ScoreImportExcelVo> list = EasyExcel.read(file.getInputStream())
                    .head(ScoreImportExcelVo.class)
                    .sheet()
                    .doReadSync();

            List<Score> scores =new ArrayList<>();
            for(ScoreImportExcelVo vo:list){
                LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Student::getStudentName,vo.getStudentName())
                        .eq(Student::getDelFlag,"0");
                // 此处查询会受租户影响，导入时需确保老师已登录且 UserContext 已设置
                List<Student> students = studentMapper.selectList(queryWrapper);

                if(students.isEmpty()){
                    return ResultVo.error("导入失败：学生["+vo.getStudentName()+"]不在本班档案中，请检查姓名或先导入学生信息!");
                }
                if(students.size()>1){
                    return ResultVo.error("导入失败：发现重名学生["+vo.getStudentName()+"],请在档案中核对身份证号");
                }
                Score score=new Score();
                score.setStudentName(vo.getStudentName());
                score.setIdCard(students.get(0).getIdCard());
                score.setClassName(students.get(0).getClassName());
                score.setSubject(vo.getSubject());
                score.setScore(vo.getScore());
                score.setExamName(examName);
                score.setDelFlag("0");
                score.setCreateTime(new Date());
                scores.add(score);
            }
            for(Score score:scores){
                scoreMapper.insert(score);
            }
            return ResultVo.success("成功导入"+scores.size()+"条成绩记录");
        }catch (IOException e){
            return ResultVo.error("解析Excel失败");
        }
    }
    /**
     * 封装成绩查询条件
     */
    private LambdaQueryWrapper<Score> buildScoreQueryWrapper(StudentQueryScoreBo bo,boolean isExact){
        LambdaQueryWrapper<Score> lqw=new LambdaQueryWrapper();
        if(bo!=null){
            if(isExact){
                //学生查：精准匹配
                lqw.eq(StringUtils.hasText(bo.getStudentName()),Score::getStudentName,bo.getStudentName());
                lqw.eq(StringUtils.hasText(bo.getIdCard()),Score::getIdCard,bo.getIdCard());
            }else{
                //老师查 模糊匹配
                lqw.like(StringUtils.hasText(bo.getStudentName()),Score::getStudentName,bo.getStudentName());
            }
        }
        lqw.eq(Score::getDelFlag,"0").orderByDesc(Score::getCreateTime);
        return lqw;
    }

    /**
     * 封装教师登录
     */
    private LambdaQueryWrapper<Admin> buildAdminQueryWrapper(AdminBo bo){
        LambdaQueryWrapper<Admin> lqw=new LambdaQueryWrapper();
        if(bo!=null){
            lqw.eq(StringUtils.hasText(bo.getUsername()),Admin::getUsername,bo.getUsername());
            lqw.eq(StringUtils.hasText(bo.getPassword()),Admin::getPassword,bo.getPassword());
        }
        lqw.eq(Admin::getDelFlag,"0");
        return lqw;
    }
}
