package com.im.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "密码不能为空")
    private String password;

    private String nickname;

    public void setUsername(String username) {
        this.username = username != null ? username.trim() : null;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname != null ? nickname.trim() : null;
    }
}
