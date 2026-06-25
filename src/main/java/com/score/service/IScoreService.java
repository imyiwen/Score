package com.score.service;

import com.score.common.ResultVo;
import com.score.entity.Admin;
import com.score.entity.Score;
import com.score.entity.Student;
import com.score.entity.bo.AdminBo;
import com.score.entity.bo.StudentQueryScoreBo;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * @author imyiwen
 * @data 2026/4/22 9:36
 */
public interface IScoreService {
    /**
     * 学生查询成绩
     */
    ResultVo<?> queryScore(StudentQueryScoreBo searchBo);

    /**
     * 教师登录
     */
    ResultVo<?> login(AdminBo adminBo);

    /**
     * 教师查询成绩
     */
    ResultVo<?> checkList(StudentQueryScoreBo searchBo);

    /**
     * 导入学生信息
     */
    ResultVo<?> importStudent(MultipartFile file,String className);

    /**
     * 导入成绩数据
     */
    ResultVo<?> importScores(MultipartFile file,String examName,String className);

    /**
     * 教师新建用户
     */
    ResultVo<?> creatAdmin(AdminBo adminBo);

    /**
     * 获取用户列表
     */
    ResultVo<?> getUserList(Integer pageNum, Integer pageSize);

    /**
     * 更新用户信息
     */
    ResultVo<?> updateUser(AdminBo adminBo);

    /**
     * 删除用户
     */
    ResultVo<?> deleteUser(String userName);
}
