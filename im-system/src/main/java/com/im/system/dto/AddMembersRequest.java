package com.im.system.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class AddMembersRequest {

    @NotEmpty(message = "成员列表不能为空")
    private List<Long> memberIds;
}