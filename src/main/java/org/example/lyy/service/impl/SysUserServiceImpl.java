package org.example.lyy.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.lyy.entity.SysUser;
import org.example.lyy.mapper.SysUserMapper;
import org.example.lyy.model.dto.LoginDTO;
import org.example.lyy.service.SysUserService;
import org.example.lyy.utils.JwtUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final String ADMIN_TOKEN_KEY = "login:admin:token:";

    public Map<String, Object> adminLogin(LoginDTO loginDTO) {
        // 1. 查询管理员
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, loginDTO.getUsername());
        SysUser sysUser = this.getOne(wrapper);

        if (sysUser == null || sysUser.getStatus() == 0) {
            throw new RuntimeException("账号不存在或已被禁用");
        }

        // 2. 密码校验 (后台密码建议也用 MD5 存)
        String encryptPassword = DigestUtil.md5Hex(loginDTO.getPassword());
        if (!encryptPassword.equals(sysUser.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        // 3. 生成带角色的 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("adminId", sysUser.getId());
        claims.put("username", sysUser.getUsername());
        claims.put("roleType", sysUser.getRoleType()); // 关键：将角色放入 Token

        String token = JwtUtils.generateToken(claims);

        // 4. 存入 Redis，有效期 12 小时
        redisTemplate.opsForValue().set(ADMIN_TOKEN_KEY + sysUser.getId(), token, 12, TimeUnit.HOURS);

        // 5. 返回给前端
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("roleType", sysUser.getRoleType());
        result.put("username", sysUser.getUsername());
        return result;
    }
}