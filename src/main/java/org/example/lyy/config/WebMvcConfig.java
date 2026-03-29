package org.example.lyy.config;

import org.example.lyy.interceptor.AdminAuthInterceptor;
import org.example.lyy.interceptor.JwtInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.annotation.Resource;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Resource
    private JwtInterceptor jwtInterceptor;

    @Resource
    private AdminAuthInterceptor adminAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**") // 拦截所有请求
                .excludePathPatterns(   // 放行以下路径
                        "/api/user/login",
                        "/api/user/register",
                        "/api/movie/**",
                        "/api/schedule/**",
                        "/api/admin/**",
                        "/error",
                        "/swagger-ui/**", "/v3/api-docs/**",
                        "/uploads/**"
                );
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")        // 拦截所有后台请求
                .excludePathPatterns("/api/admin/sys/login"); // 放行后台登录接口
    }
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 获取项目运行的根目录
        String projectPath = System.getProperty("user.dir");
        // 将 /uploads/** 的请求映射到本地物理路径
        // 注意：Windows 和 Linux 下路径格式要求 file: 协议，末尾需要加斜杠
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + projectPath + "/uploads/");
    }
}