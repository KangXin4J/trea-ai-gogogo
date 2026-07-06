package com.im.system.dto;

import lombok.Data;

@Data
public class UpdateUserRequest {

    private String nickname;

    private String avatar;

    private String signature;
}
