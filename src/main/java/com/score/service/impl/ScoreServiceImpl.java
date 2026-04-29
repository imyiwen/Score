package com.score.service.impl;

import cn.dev33.satoken.annotation.SaCheckLogin;
import cn.dev33.satoken.stp.StpUtil;
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
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ScoreServiceImpl implements IScoreService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private ScoreMapper scoreMapper;
    @Autowired
    private StudentMapper studentMapper;

    @Override
    public ResultVo<?> queryScore(StudentQueryScoreBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getStudentName())) {
            return ResultVo.error("查询失败：姓名不能为空");
        }
        if (!StringUtils.hasText(bo.getIdCard())) {
            return ResultVo.error("查询失败：身份证号不能为空");
        }
        LambdaQueryWrapper<Score> lqw = buildScoreQueryWrapper(bo, true);
        return ResultVo.success(scoreMapper.studentQuery(lqw));
    }

    @Override
    public ResultVo<?> checkList(StudentQueryScoreBo bo) {
        // 多租户隔离由拦截器自动注入上下文，插件自动拦截
        Page<Score> page = new Page<>(bo.getPageNum(), bo.getPageSize());
        LambdaQueryWrapper<Score> lqw = buildScoreQueryWrapper(bo, false);
        return ResultVo.success(scoreMapper.selectPage(page, lqw));
    }

    @Override
    public ResultVo<?> login(AdminBo bo) {
        ServletRequestAttributes attributes=(ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if(attributes==null){
            return ResultVo.error("系统繁忙，请稍后再试");
        }
        HttpSession session = attributes.getRequest().getSession();
        String captchaId =bo.getCaptchaId();
        if(captchaId==null||captchaId.isEmpty()){
            return ResultVo.error("安全验证标识不能为空：请刷新页面");
        }
        Boolean verified = (Boolean) session.getAttribute("captcha_verified_"+captchaId);
        if(verified==null||!verified){
            return ResultVo.error("安全验证未通过，请重试");
        }
        session.removeAttribute("captcha_verified_"+captchaId);
        log.info("验证成功");
        if (bo == null || !StringUtils.hasText(bo.getUserName())) {
            return ResultVo.error("登录失败：用户名不能为空");
        }

        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, bo.getUserName()).eq(Admin::getDelFlag, "0");
        Admin admin = adminMapper.selectOne(lqw);

        if (admin == null) {
            return ResultVo.error("登录失败：用户名或密码错误");
        }

        // 3. 校验密码（支持 BCrypt）
        if (!BCrypt.checkpw(bo.getPassword(), admin.getPassword())) {
            return ResultVo.error("登录失败：用户名或密码错误");
        }

        // 4. 登录成功
        StpUtil.login(admin.getUserName());
        StpUtil.getSession().set("className", admin.getClassName());
        return ResultVo.success(StpUtil.getTokenValue()+"登陆成功");
    }

    @Override
    public ResultVo<?> importStudent(MultipartFile file, String className) {
        String realClassName = (String) StpUtil.getSession().get("className");
        try {
            EasyExcel.read(file.getInputStream(), StudentImportExcelVo.class, 
                    new StudentImportListener(studentMapper, realClassName))
                    .sheet()
                    .doRead();
            return ResultVo.success("学生信息导入成功");
        } catch (IOException e) {
            log.error("导入异常", e);
            return ResultVo.error("解析Excel失败");
        }
    }

    @Override
    public ResultVo<?> importScores(MultipartFile file, String examName, String className) {
        String realClassName = (String) StpUtil.getSession().get("className");
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
                score.setClassName(realClassName);
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
        }
    }

    @Override
    public ResultVo<?> creatAdmin(AdminBo bo) {
        if(bo==null||!StringUtils.hasText(bo.getUserName())||!StringUtils.hasText(bo.getPassword())||!StringUtils.hasText(bo.getClassName())){
            return ResultVo.error("创建失败：用户名或密码或班级不能为空！");
        }
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, bo.getUserName()).eq(Admin::getDelFlag, "0");
        if(adminMapper.selectCount(lqw) > 0){
            return ResultVo.error("创建失败：用户名：【"+bo.getUserName()+"】，已存在！");
        }
        Admin admin = new Admin();
        admin.setUserName(bo.getUserName());
        String hashPw = BCrypt.hashpw(bo.getPassword(), BCrypt.gensalt());
        admin.setPassword(hashPw);
        admin.setClassName(bo.getClassName());
        admin.setCreateTime(new Date());
        admin.setDelFlag("0");
        int rows = adminMapper.insert(admin);
        return rows > 0 ? ResultVo.success("创建成功！") : ResultVo.error("创建失败");
    }

    @Override
    public ResultVo<?> getUserList(Integer pageNum, Integer pageSize) {
        Page<Admin> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getDelFlag, "0").orderByDesc(Admin::getCreateTime);
        return ResultVo.success(adminMapper.selectPage(page, lqw));
    }

    @Override
    public ResultVo<?> updateUser(AdminBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getUserName())) {
            return ResultVo.error("更新失败：用户名不能为空");
        }
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, bo.getUserName()).eq(Admin::getDelFlag, "0");
        Admin admin = adminMapper.selectOne(lqw);
        if (admin == null) {
            return ResultVo.error("更新失败：用户不存在");
        }
        admin.setClassName(bo.getClassName());
        int rows = adminMapper.updateById(admin);
        return rows > 0 ? ResultVo.success("更新成功") : ResultVo.error("更新失败");
    }

    @Override
    public ResultVo<?> deleteUser(String userName) {
        if (!StringUtils.hasText(userName)) {
            return ResultVo.error("删除失败：用户名不能为空");
        }
        LambdaQueryWrapper<Admin> lqw = new LambdaQueryWrapper<>();
        lqw.eq(Admin::getUserName, userName).eq(Admin::getDelFlag, "0");
        Admin admin = adminMapper.selectOne(lqw);
        if (admin == null) {
            return ResultVo.error("删除失败：用户不存在");
        }
        admin.setDelFlag("1");
        int rows = adminMapper.updateById(admin);
        return rows > 0 ? ResultVo.success("删除成功") : ResultVo.error("删除失败");
    }


    private LambdaQueryWrapper<Score> buildScoreQueryWrapper(StudentQueryScoreBo bo, boolean isExact) {
        LambdaQueryWrapper<Score> lqw = new LambdaQueryWrapper<>();
        if (bo != null) {
            // 注意：className 的过滤现在由多租户插件自动完成，无需手动拼接
            if (isExact) {
                lqw.eq(StringUtils.hasText(bo.getStudentName()), Score::getStudentName, bo.getStudentName());
                lqw.eq(StringUtils.hasText(bo.getIdCard()), Score::getIdCard, bo.getIdCard());
            } else {
                lqw.like(StringUtils.hasText(bo.getStudentName()), Score::getStudentName, bo.getStudentName());
                lqw.like(StringUtils.hasText(bo.getExamName()), Score::getExamName, bo.getExamName());
            }
        }
        lqw.eq(Score::getDelFlag, "0").orderByDesc(Score::getCreateTime);
        return lqw;
    }
}
