package com.score.mapper;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.score.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @author imyiwen
 * @data 2026/4/22 10:53
 */
@Mapper
public interface StudentMapper extends BaseMapper<Student> {
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT COUNT(*) FROM student where student_name=#{studentName} AND id_card=#{idCard} AND del_flag='0'")
    Integer checkStudent(@Param("studentName") String studentName,@Param("idCard") String idCard);
}
