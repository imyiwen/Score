package com.score.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.score.common.UserContext;
import com.score.entity.Admin;
import com.score.mapper.AdminMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 登录拦截器：自动提取用户信息并装载多租户上下文
 */
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private AdminMapper adminMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 从 Header 中获取当前登录用户名
        String userName = request.getHeader("Authorization");
        
        if (StringUtils.hasText(userName)) {
            LambdaQueryWrapper<Admin> query = new LambdaQueryWrapper<>();
            query.eq(Admin::getUserName, userName).eq(Admin::getDelFlag, "0");
            Admin admin = adminMapper.selectOne(query);
            
            if (admin != null) {
                // 自动注入 ThreadLocal 上下文，供 MyBatis Plus 插件使用
                UserContext.setClassName(admin.getClassName());
                UserContext.setUserName(userName);
            }
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        // 请求结束，必须清理上下文，防止内存泄漏和线程复用导致的数据污染
        UserContext.remove();
    }
}
