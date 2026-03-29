package org.example.lyy.interceptor;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.lyy.annotation.RequireRole;
import org.example.lyy.utils.JwtUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.annotation.Resource;
import java.util.Arrays;

@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果不是映射到方法上的请求（例如静态资源），直接放行
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        String token = request.getHeader("Authorization");
        if (!StringUtils.hasText(token)) {
            response.setStatus(401);
            return false;
        }

        try {
            // 1. 解析 Token 获取角色和 ID
            Claims claims = JwtUtils.parseToken(token);
            Long adminId = claims.get("adminId", Long.class);
            String roleType = claims.get("roleType", String.class);

            // 2. 检查 Redis 单点登录/是否退出
            String redisToken = (String) redisTemplate.opsForValue().get("login:admin:token:" + adminId);
            if (!StringUtils.hasText(redisToken) || !redisToken.equals(token)) {
                response.setStatus(401);
                return false;
            }

            // 3. RBAC 权限校验 (核心逻辑)
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            // 先尝试从方法上获取注解
            RequireRole requireRole = handlerMethod.getMethodAnnotation(RequireRole.class);
            // 如果方法上没有，尝试从类上获取
            if (requireRole == null) {
                requireRole = handlerMethod.getBeanType().getAnnotation(RequireRole.class);
            }

            // 如果打上了 @RequireRole 注解，则必须检查角色
            if (requireRole != null) {
                boolean hasPermission = Arrays.asList(requireRole.value()).contains(roleType);
                if (!hasPermission) {
                    response.setStatus(403); // 403 Forbidden 无权限访问
                    return false;
                }
            }

            // 将 adminId 存入 request 方便后续使用
            request.setAttribute("currentAdminId", adminId);
            request.setAttribute("currentRole", roleType);
            return true;

        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }
}