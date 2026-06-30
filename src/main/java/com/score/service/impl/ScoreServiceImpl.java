package com.score.service.impl;

import cloud.tianai.captcha.application.ImageCaptchaApplication;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.score.Listener.StudentImportListener;
import com.score.common.ResultVo;
import com.score.entity.Admin;
import com.score.entity.Score;
import com.score.entity.Student;
import com.score.entity.bo.AdminBo;
import com.score.entity.bo.StudentQueryScoreBo;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * @author imyiwen
 * @data 2026/4/22 9:36
 */

@Slf4j
@Service
public class ScoreServiceImpl implements IScoreService {

    @Autowired
    private AdminMapper adminMapper;
    @Autowired
    private ScoreMapper scoreMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private ImageCaptchaApplication imageCaptchaApplication;

    @Override
    public ResultVo<?> queryScore(StudentQueryScoreBo bo) {
        if (bo == null || !StringUtils.hasText(bo.getStudentName())) {
            return ResultVo.error("查询失败：姓名不能为空");
        }
        // 校验身份证号格式已由 @Pattern 注解处理
        //身份验证
        if(studentMapper.checkStudent(bo.getStudentName(),bo.getIdCard())==0){
            return ResultVo.error("姓名或身份证号错误，请检查！");
        }
        LambdaQueryWrapper<Score> lqw = buildScoreQueryWrapper(bo, true);
        List<Score> scores = scoreMapper.studentQuery(lqw);
        BigDecimal totalScore = scores.stream().map(s->s.getScore() !=null?s.getScore():BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String,Object> result=new HashMap<>();
        result.put("score",scores);
        result.put("totalScore",totalScore);
        result.put("subjectCount",scores.size());
        return ResultVo.success(result);
    }

    @Override
    public ResultVo<?> checkList(StudentQueryScoreBo bo) {
        // 多租户隔离由拦截器自动注入上下文，插件自动拦截

        LambdaQueryWrapper<Score> lqw =buildScoreQueryWrapper(bo, false);
        List<Score> rawList = scoreMapper.selectList(lqw);
        Map<String,Map<String,Object>> groupMap =new LinkedHashMap<>();
        for(Score s:rawList){
            // 用学生姓名+考试名作为分组键
            String rowKey = s.getStudentName() + "|" + s.getExamName();
            Map<String,Object> row =groupMap.computeIfAbsent(rowKey,k->{Map<String,Object> map=new HashMap<>();
            map.put("studentName",s.getStudentName());
            map.put("examName",s.getExamName());
            map.put("className",s.getClassName());
            map.put("createTime",s.getCreateTime());
            map.put("subjects",new HashMap<String, BigDecimal>());
            map.put("totalScore",BigDecimal.ZERO);
            return map;});
            Map<String,BigDecimal> subjects=(Map<String,BigDecimal>)row.get("subjects");
            BigDecimal score = s.getScore() != null ? s.getScore() : BigDecimal.ZERO;
            subjects.put(s.getSubject(), score);
            BigDecimal currentTotal=(BigDecimal)row.get("totalScore");
            row.put("totalScore",currentTotal.add(score));
        }
        return ResultVo.success(new ArrayList<>(groupMap.values()));
    }

    @Override
    public ResultVo<?> login(AdminBo bo) {

        if (bo == null || !StringUtils.hasText(bo.getUserName())||!StringUtils.hasText(bo.getPassword())) {
            return ResultVo.error("登录失败：用户名或密码不能为空");
        }
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
        StpUtil.getSession().set("userName",admin.getUserName());
        String token = StpUtil.getTokenValue();
        Map<String,Object>result=new HashMap<>();
        result.put("token",token);
        return ResultVo.success(result);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultVo<?> importStudent(MultipartFile file, String className) {
        // 校验文件类型
        String contentType = file.getContentType();
        if (contentType == null || !contentType.contains("excel")) {
            return ResultVo.error("请上传Excel文件");
        }

        // 先将文件读入字节数组
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }

        // 校验文件大小（最大 10MB）
        if (fileBytes.length > 10 * 1024 * 1024) {
            return ResultVo.error("文件大小不能超过10MB");
        }

        // 读取Excel
        try {
            EasyExcel.read(new java.io.ByteArrayInputStream(fileBytes), StudentImportExcelVo.class,
                    new StudentImportListener(studentMapper, className))
                    .sheet()
                    .doRead();
        } catch (Exception e) {
            log.error("学生导入异常", e);
            throw new RuntimeException("解析Excel失败：" + e.getMessage());
        }

        return ResultVo.success("学生信息导入成功");
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public ResultVo<?> importScores(MultipartFile file, String examName, String className) {
        // 先将文件读入字节数组，避免多次调用 getInputStream()（流只能读一次）
        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败", e);
        }

        // 读取表头（第一行）
        List<Map<Integer, String>> headList = EasyExcel.read(new java.io.ByteArrayInputStream(fileBytes))
                .sheet().headRowNumber(0).doReadSync();
        if (headList.isEmpty()) return ResultVo.error("Excel文件为空");

        // 根据表头列名建立 列名->列索引 的映射
        Map<String, Integer> headMap = new HashMap<>();
        for (Map.Entry<Integer, String> entry : headList.get(0).entrySet()) {
            headMap.put(entry.getValue(), entry.getKey());
        }

        // 校验必要列
        if (!headMap.containsKey("姓名")) {
            return ResultVo.error("Excel表头缺少'姓名'列");
        }
        Integer nameColIndex = headMap.get("姓名");

        // 读取数据部分
        List<Map<Integer, String>> dataList = EasyExcel.read(new java.io.ByteArrayInputStream(fileBytes))
                .sheet().headRowNumber(1).doReadSync();

        int successCount = 0;
        for (Map<Integer, String> data : dataList) {
            String studentName = data.get(nameColIndex);
            if (!StringUtils.hasText(studentName)) continue;

            // 查询学生身份证号
            LambdaQueryWrapper<Student> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Student::getStudentName, studentName).eq(Student::getClassName, className).eq(Student::getDelFlag, "0");
            Student student = studentMapper.selectOne(queryWrapper);
            if (student == null) {
                log.warn("学生：{} 不在班级：{} 档案中，跳过", studentName, className);
                continue;
            }

            // 遍历表头中的所有科目列（跳过"姓名"列）
            for (Map.Entry<String, Integer> entry : headMap.entrySet()) {
                if ("姓名".equals(entry.getKey())) continue;
                String subject = entry.getKey();
                String scoreStr = data.get(entry.getValue());
                if (StringUtils.hasText(subject) && StringUtils.hasText(scoreStr)) {
                    Score score = new Score();
                    score.setStudentName(studentName);
                    score.setIdCard(student.getIdCard());
                    score.setClassName(className);
                    score.setSubject(subject);
                    score.setScore(new BigDecimal(scoreStr));
                    score.setExamName(examName);
                    score.setDelFlag("0");
                    score.setCreateTime(new Date());

                    scoreMapper.insert(score);
                    successCount++;
                }
            }
        }
        return ResultVo.success("成功导入：" + successCount + "条成绩记录");
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
