package com.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    private Long id;

    /**
     * 关键修复：允许 Java 使用 userName，但映射到数据库全小写 username
     */
    @TableField("username")
    private String userName;

    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;
    private String className;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
    private String delFlag;
}
