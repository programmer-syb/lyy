package org.example.lyy.controller;

import org.example.lyy.common.Result;
import org.example.lyy.model.dto.LoginDTO;
import org.example.lyy.service.SysUserService;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/sys")
public class SysUserController {

    @Resource
    private SysUserService sysUserService;

    @PostMapping("/login")
    public Result<Map<String, Object>> adminLogin(@RequestBody LoginDTO loginDTO) {
        return Result.success(sysUserService.adminLogin(loginDTO));
    }
}