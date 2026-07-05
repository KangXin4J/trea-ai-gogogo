package com.im.system.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMemberDTO {

    private Long userId;

    private String username;

    private String nickname;

    private String avatar;
}