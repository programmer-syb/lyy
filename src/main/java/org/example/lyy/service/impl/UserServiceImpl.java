package org.example.lyy.service.impl;

import cn.hutool.crypto.digest.DigestUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.example.lyy.entity.User;
import org.example.lyy.mapper.UserMapper;
import org.example.lyy.model.dto.LoginDTO;
import org.example.lyy.model.vo.LoginVO;
import org.example.lyy.service.UserService;
import org.example.lyy.utils.JwtUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    // Redis 中存储 token 的 key 前缀
    private static final String LOGIN_USER_KEY = "login:user:token:";

    public LoginVO login(LoginDTO loginDTO) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, loginDTO.getUsername());
        User user = this.getOne(wrapper);

        // 2. 校验用户是否存在
        if (user == null) {
            throw new RuntimeException("用户名或密码错误"); // 后期可替换为全局自定义异常
        }

        // 3. 校验密码 (数据库存的是加密后的密码，将前端传来的密码加密后比对)
        String encryptPassword = DigestUtil.md5Hex(loginDTO.getPassword());
        if (!encryptPassword.equals(user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("username", user.getUsername());
        String token = JwtUtils.generateToken(claims);

        // 5. 将 Token 存入 Redis，设置 24 小时过期
        // 这样做的目的是：如果在 Redis 里找不到这个 token，说明用户已退出登录或 token 被我们手动作废了
        redisTemplate.opsForValue().set(LOGIN_USER_KEY + user.getId(), token, 24, TimeUnit.HOURS);

        // 6. 封装返回结果
        LoginVO loginVO = new LoginVO();
        BeanUtils.copyProperties(user, loginVO);
        loginVO.setToken(token);

        return loginVO;
    }

    @Override
    public void register(LoginDTO loginDTO) {
        String username = loginDTO.getUsername();
        String password = loginDTO.getPassword();

        // 1. 校验用户名是否已存在
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getUsername, username);
        User existUser = this.getOne(queryWrapper);

        if (existUser != null) {
            throw new RuntimeException("用户名已存在，请更换用户名");
        }

        // 2. 密码加密（核心！不能存明文）
        String encryptPassword = DigestUtil.md5Hex(password);

        // 3. 构建用户对象
        User user = new User();
        user.setUsername(username);
        user.setPassword(encryptPassword);
        user.setNickname("用户_" + username); // 默认昵称
        user.setGender(0); // 0 未知
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        user.setIsDeleted(0);

        // 4. 保存到数据库
        this.save(user);
    }
}