package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.common.Result;
import com.im.system.dto.AddMembersRequest;
import com.im.system.dto.ConversationMemberDTO;
import com.im.system.dto.CreateConversationRequest;
import com.im.system.entity.Conversation;
import com.im.system.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;
    private final JwtUtil jwtUtil;

    @GetMapping
    public Result<List<Conversation>> getUserConversations(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<Conversation> conversations = conversationService.getUserConversations(userId);
        return Result.success(conversations);
    }

    @GetMapping("/{id}")
    public Result<Conversation> getConversationById(@PathVariable Long id,
                                                    HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        Conversation conversation = conversationService.getConversationById(userId, id);
        return Result.success(conversation);
    }

    @PostMapping
    public Result<Conversation> createConversation(@Valid @RequestBody CreateConversationRequest request,
                                                   HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        Conversation conversation = conversationService.createConversation(userId, request);
        return Result.success(conversation);
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteConversation(@PathVariable Long id,
                                           HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        conversationService.deleteConversation(userId, id);
        return Result.success();
    }

    @GetMapping("/{id}/members")
    public Result<List<ConversationMemberDTO>> getConversationMembers(@PathVariable Long id,
                                                                       HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<ConversationMemberDTO> members = conversationService.getConversationMembers(userId, id);
        return Result.success(members);
    }

    @PostMapping("/{id}/members")
    public Result<List<ConversationMemberDTO>> addMembers(@PathVariable Long id,
                                                          @Valid @RequestBody AddMembersRequest request,
                                                          HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        List<ConversationMemberDTO> members = conversationService.addMembers(userId, id, request);
        return Result.success(members);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new RuntimeException("未授权");
    }
}