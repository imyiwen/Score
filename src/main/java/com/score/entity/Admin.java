package com.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * @author imyiwen
 * @data 2026/4/21 16:59
 */
@Data
@TableName("admin")
public class Admin {
    @TableId(type = IdType.AUTO)
    /**
     * 主键Id
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;
    /**
     * 班级名
     */
    private String manageClass;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    /**
     * 删除标识（0:正常、1:删除）
     */
    private String delFlag;
}
