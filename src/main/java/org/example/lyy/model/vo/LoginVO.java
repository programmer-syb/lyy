package org.example.lyy.model.vo;

import lombok.Data;

@Data
public class LoginVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String token; // 颁发给前端的 JWT Token
}