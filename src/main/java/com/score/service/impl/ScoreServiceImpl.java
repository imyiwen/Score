package com.score.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.score.Listener.StudentImportListener;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ScoreServiceImpl implements IScoreService {

    @Resource
    private AdminMapper adminMapper;
    @Resource
    private ScoreMapper scoreMapper;
    @Autowired
    private StudentMapper studentMapper;

    @Override
    public ResultVo<?> queryScore(StudentQueryScoreBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getStudentName())) {
            return ResultVo.error("查询失败：姓名不能为空");
        }
        if (bo == null || !StringUtils.hasText(bo.getIdCard())) {
            return ResultVo.error("查询失败：身份证号不能为空");
        }
        LambdaQueryWrapper<Score> lqw = buildScoreQueryWrapper(bo, true);
        return ResultVo.success(scoreMapper.selectList(lqw));
    }

    @Override
    public ResultVo<?> checkList(StudentQueryScoreBo bo) {
        if (bo != null) {
            if (StringUtils.hasText(bo.getClassName())) {
                UserContext.setClassName(bo.getClassName());
            }
            if (StringUtils.hasText(bo.getUserName())) {
                UserContext.setUserName(bo.getUserName());
            }
        }
        try {
            Page<Score> page = new Page<>(bo.getPageNum(), bo.getPageSize());
            LambdaQueryWrapper<Score> lqw = buildScoreQueryWrapper(bo, false);
            return ResultVo.success(scoreMapper.selectPage(page, lqw));
        } finally {
            UserContext.remove();
        }
    }

    @Override
    public ResultVo<?> login(AdminBo bo) {
        LambdaQueryWrapper<Admin> lqw = buildAdminQueryWrapper(bo);
        return ResultVo.success(adminMapper.selectOne(lqw));
    }

    @Override
    public ResultVo<?> importStudent(MultipartFile file, String className) {
        UserContext.setClassName(className);
        try {
            EasyExcel.read(file.getInputStream(), StudentImportExcelVo.class, 
                    new StudentImportListener(studentMapper, className))
                    .sheet()
                    .doRead();
            return ResultVo.success("学生信息导入成功");
        } catch (IOException e) {
            log.error("导入异常", e);
            return ResultVo.error("解析Excel失败");
        } finally {
            UserContext.remove();
        }
    }

    @Override
    public ResultVo<?> importScores(MultipartFile file, String examName, String className) {
        UserContext.setClassName(className);
        try {
            List<ScoreImportExcelVo> list = EasyExcel.read(file.getInputStream())
                    .head(ScoreImportExcelVo.class)
                    .sheet()
                    .doReadSync();

            List<Score> scores = new ArrayList<>();
            String lastStudentName = "";

            for (ScoreImportExcelVo vo : list) {
                if (vo == null || (!StringUtils.hasText(vo.getStudentName()) && !StringUtils.hasText(vo.getSubject()))) {
                    continue;
                }
                if (!StringUtils.hasText(vo.getStudentName())) {
                    vo.setStudentName(lastStudentName);
                } else {
                    lastStudentName = vo.getStudentName();
                }

                LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Student::getStudentName, vo.getStudentName())
                        .eq(Student::getDelFlag, "0");
                List<Student> students = studentMapper.selectList(queryWrapper);

                if (students.isEmpty()) {
                    log.warn("学生[{}]不在档案中", vo.getStudentName());
                    continue;
                }
                
                Score score = new Score();
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
            
            for (Score score : scores) {
                scoreMapper.insert(score);
            }
            return ResultVo.success("成功导入" + scores.size() + "条成绩记录");
        } catch (IOException e) {
            log.error("解析异常", e);
            return ResultVo.error("解析Excel失败");
        } finally {
            UserContext.remove();
        }
    }

    public ResultVo<?> creatAdmin(AdminBo bo) {
        if(bo==null||!StringUtils.hasText(bo.getUserName())||!StringUtils.hasText(bo.getPassword())||!StringUtils.hasText(bo.getClassName())){
            return ResultVo.error("创建失败：用户名或密码或班级不能为空！");
        }
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, bo.getUserName())
                .eq(Admin::getDelFlag, "0");
        if(adminMapper.selectCount(lqw)>0){
            return ResultVo.error("创建失败：用户名：【"+bo.getUserName()+"】，已存在！");
        }
        Admin admin = new Admin();
        admin.setUserName(bo.getUserName());
        admin.setPassword(bo.getPassword());
        admin.setClassName(bo.getClassName());
        admin.setCreateTime(new Date());
        admin.setDelFlag("0");
        int rows = adminMapper.insert(admin);
        return rows >0 ? ResultVo.success("创建成功！"):ResultVo.error("创建失败");
    }

    private LambdaQueryWrapper<Score> buildScoreQueryWrapper(StudentQueryScoreBo bo, boolean isExact) {
        LambdaQueryWrapper<Score> lqw = new LambdaQueryWrapper<>();
        if (bo != null) {
            if (isExact) {
                lqw.eq(StringUtils.hasText(bo.getStudentName()), Score::getStudentName, bo.getStudentName());
                lqw.eq(StringUtils.hasText(bo.getIdCard()), Score::getIdCard, bo.getIdCard());
            } else {
                lqw.like(StringUtils.hasText(bo.getStudentName()), Score::getStudentName, bo.getStudentName());
            }
        }
        lqw.eq(Score::getDelFlag, "0").orderByDesc(Score::getCreateTime);
        return lqw;
    }

    private LambdaQueryWrapper<Admin> buildAdminQueryWrapper(AdminBo bo) {
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        if (bo != null) {
            // 这里使用了 Admin::getUserName，配合 @TableField("username") 注解，就能完美运行
            lqw.eq(StringUtils.hasText(bo.getUserName()), Admin::getUserName, bo.getUserName());
            lqw.eq(StringUtils.hasText(bo.getPassword()), Admin::getPassword, bo.getPassword());
        }
        lqw.eq(Admin::getDelFlag, "0");
        return lqw;
    }
}
