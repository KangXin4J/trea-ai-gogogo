package com.im.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {

    private Long receiverId;

    @NotBlank(message = "消息内容不能为空")
    private String content;

    private String type;

    private Long conversationId;
}
