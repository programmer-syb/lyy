package org.example.lyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.lyy.entity.SysUser;
import org.example.lyy.model.dto.LoginDTO;

import java.util.Map;

public interface SysUserService extends IService<SysUser> {
    Map<String, Object> adminLogin(LoginDTO loginDTO);
}