package com.score.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.score.entity.Score;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @author imyiwen
 * @data 2026/4/22 9:50
 */

@Mapper
public interface ScoreMapper extends BaseMapper<Score> {
    //忽略租户隔离
    @InterceptorIgnore(tenantLine="true")
    @Select("SELECT * FROM score ${ew.customSqlSegment}")
    List<Score> studentQuery(@Param(Constants.WRAPPER) Wrapper<Score> queryWrapper);
}
