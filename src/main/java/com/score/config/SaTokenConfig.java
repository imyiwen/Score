package com.score.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 指定拦截规则
            SaRouter.match("/**")
                    .notMatch(
                            "/login",
                            "/StudentCheck",
                            "/scoreCheck/login",
                            "/scoreCheck/StudentCheck"
                    ) // 排除登录和学生查询接口
                    .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
