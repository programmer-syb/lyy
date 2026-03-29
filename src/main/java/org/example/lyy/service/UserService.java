package org.example.lyy.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.example.lyy.entity.User;
import org.example.lyy.model.dto.LoginDTO;
import org.example.lyy.model.vo.LoginVO;

public interface UserService extends IService<User> {
    LoginVO login(LoginDTO loginDTO);

    void register(LoginDTO loginDTO);
}