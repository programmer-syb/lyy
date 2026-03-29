package org.example.lyy.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.lyy.utils.JwtUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 从请求头中获取 Token (通常前端会将 Token 放在 Authorization 字段中)
        String token = request.getHeader("Authorization");

        // 如果没有 token，直接拦截并返回 401 状态码
        if (!StringUtils.hasText(token)) {
            response.setStatus(401);
            return false;
        }

        try {
            // 2. 解析 Token
            Claims claims = JwtUtils.parseToken(token);
            Long userId = claims.get("userId", Long.class);

            // 3. 校验 Redis 中是否存在该 Token (实现退出登录、单点登录踢人的关键)
            String redisToken = (String) redisTemplate.opsForValue().get("login:user:token:" + userId);
            if (!StringUtils.hasText(redisToken) || !redisToken.equals(token)) {
                response.setStatus(401);
                return false;
            }

            // 4. 将 userId 存入 request 作用域中，方便后续 Controller 直接获取当前登录用户
            request.setAttribute("currentUserId", userId);
            return true;

        } catch (Exception e) {
            // Token 过期或解析失败
            response.setStatus(401);
            return false;
        }
    }
}