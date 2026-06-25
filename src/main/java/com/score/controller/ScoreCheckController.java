package com.score.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.score.common.ResultVo;
import com.score.entity.bo.AdminBo;
import com.score.entity.bo.StudentQueryScoreBo;
import com.score.service.IScoreService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/scoreCheck")
@Slf4j
public class ScoreCheckController {

    @Autowired
    private IScoreService scoreService;

    @PostMapping("/StudentCheck")
    public ResultVo<?> studentQuery(@Valid @RequestBody StudentQueryScoreBo bo) {
        log.info("学生：{}{}|查询成绩",bo.getStudentName(),bo.getClassName());
        return scoreService.queryScore(bo);
    }

    @PostMapping("/login")
    public ResultVo<?> login(@Valid @RequestBody AdminBo bo){
        log.info("教师：{}|登录系统", bo.getUserName());
        return scoreService.login(bo);
    }

    @PostMapping("/checkList")
    public ResultVo<?> checkList(@Valid @RequestBody StudentQueryScoreBo searchBo){
        log.info("教师：{} | 查询成绩", StpUtil.getSession().get("userName"));
        return scoreService.checkList(searchBo);
    }

    @PostMapping("/importStudent")
    public ResultVo<?> importStudent(@RequestParam("file") MultipartFile file,@RequestParam("className") String className){
        log.info("导入班级：{}|学生信息",StpUtil.getSession().get("userName"), className);
        return scoreService.importStudent(file,className);
    }

    @PostMapping("/importScore")
    public ResultVo<?> importScore(@RequestParam("file") MultipartFile file, @RequestParam("examName") String examName, @RequestParam("className") String className){
        log.info("导入考试：{}|成绩数据", StpUtil.getSession().get("userName"), examName);
        return scoreService.importScores(file,examName,className);
    }

    @PostMapping("/creatAdmin")
    public ResultVo<?> creatAdmin(@Valid @RequestBody AdminBo adminBo){
        log.info("教师：{}|创建用户:{}",StpUtil.getSession().get("userName"), adminBo.getUserName());
        return scoreService.creatAdmin(adminBo);
    }

    @GetMapping("/getUserList")
    public ResultVo<?> getUserList(@RequestParam(defaultValue = "1") Integer pageNum, @RequestParam(defaultValue = "10") Integer pageSize) {
        return scoreService.getUserList(pageNum, pageSize);
    }

    @PostMapping("/updateUser")
    public ResultVo<?> updateUser(@Valid @RequestBody AdminBo adminBo) {
        return scoreService.updateUser(adminBo);
    }

    @DeleteMapping("/{userName}")
    public ResultVo<?> deleteUser(@PathVariable String userName) {
        return scoreService.deleteUser(userName);
    }

    @PostMapping("/logout")
    public ResultVo<?> logout() {
        StpUtil.logout();
        return ResultVo.success("退出登录成功");
    }
}
