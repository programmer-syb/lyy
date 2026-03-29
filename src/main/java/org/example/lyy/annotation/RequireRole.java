package org.example.lyy.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义角色权限校验注解
 */
@Target({ElementType.METHOD, ElementType.TYPE}) // 可以加在方法上，也可以加在整个 Controller 类上
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRole {
    // 允许访问的角色列表，例如 {"SUPER_ADMIN", "MANAGER"}
    String[] value(); 
}