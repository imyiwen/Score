package com.score.controller;

import com.score.common.ResultVo;
import com.score.entity.bo.AdminBo;
import com.score.entity.bo.StudentQueryScoreBo;
import com.score.service.IScoreService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author imyiwen
 * @data 2026/4/21 16:55
 */
@RestController
@RequestMapping("/scoreCheck")
@Slf4j
public class ScoreCheckController {

    @Autowired
    private IScoreService scoreService;
    /**
     * 学生查询成绩（不需要登录）
     */
    @PostMapping("/StudentCheck")
    public ResultVo<?> studentQuery(@RequestBody StudentQueryScoreBo bo) {
        log.info("学生：{}|查询成绩",bo);
        return scoreService.queryScore(bo);
    }

    /**
     * 教师登录
     */
    @PostMapping("/login")
    public ResultVo<?> login(@RequestBody AdminBo bo){
        log.info("教师：{}|登录系统",bo);
        return scoreService.login(bo);
    }

    /**
     * 教师查询成绩
     */
    @PostMapping("/checkList")
    public ResultVo<?> checkList(@RequestBody StudentQueryScoreBo searchBo){
        log.info("教师：{}|查询成绩");
        return scoreService.checkList(searchBo);
    }
    /**
     * 导入学生信息
     */
    @PostMapping("/importStudent")
    public ResultVo<?> importStudent(@RequestParam("file") MultipartFile file,@RequestParam("className") String className){
        log.info("导入班级：{}|学生信息",className);
        return scoreService.importStudent(file,className);
    }

    /**
     * 导入学生成绩
     */
    @PostMapping("/importScore")
    public ResultVo<?> importScore(@RequestParam("file") MultipartFile file, @RequestParam("examName") String examName){
        log.info("导入考试：{}|成绩数据",examName);
        return scoreService.importScores(file,examName);
    }

}
