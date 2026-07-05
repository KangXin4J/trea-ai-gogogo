package com.im.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class CreateConversationRequest {

    @NotBlank(message = "会话类型不能为空")
    private String type;

    @NotEmpty(message = "成员列表不能为空")
    private List<Long> memberIds;

    private String name;
}