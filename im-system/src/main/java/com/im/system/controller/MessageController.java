package com.im.system.controller;

import com.im.system.common.JwtUtil;
import com.im.system.common.Result;
import com.im.system.dto.PageResponse;
import com.im.system.dto.SendMessageRequest;
import com.im.system.entity.Message;
import com.im.system.service.MessageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;
    private final JwtUtil jwtUtil;

    @PostMapping("/send")
    public Result<Message> sendMessage(@Valid @RequestBody SendMessageRequest request,
                                       HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        Message message = messageService.sendMessage(userId, request);
        return Result.success(message);
    }

    @GetMapping("/conversation/{userId}")
    public Result<List<Message>> getConversationMessages(@PathVariable Long userId,
                                                         HttpServletRequest httpRequest) {
        Long currentUserId = getUserIdFromRequest(httpRequest);
        List<Message> messages = messageService.getConversationMessages(currentUserId, userId);
        return Result.success(messages);
    }

    @GetMapping("/unread-count")
    public Result<Long> getUnreadCount(HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        return Result.success(messageService.getUnreadCount(userId));
    }

    @PostMapping("/mark-read/{conversationId}")
    public Result<Void> markAsRead(@PathVariable Long conversationId,
                                   HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        messageService.markAsRead(userId, conversationId);
        return Result.success();
    }

    @GetMapping("/conversation/{conversationId}/paged")
    public Result<PageResponse<Message>> getMessagesByConversationIdPaged(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        PageResponse<Message> messages = messageService.getMessagesByConversationIdPaged(userId, conversationId, page, size);
        return Result.success(messages);
    }

    @GetMapping("/conversation/{conversationId}/search")
    public Result<PageResponse<Message>> searchMessages(
            @PathVariable Long conversationId,
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest httpRequest) {
        Long userId = getUserIdFromRequest(httpRequest);
        PageResponse<Message> messages = messageService.searchMessages(userId, conversationId, keyword, page, size);
        return Result.success(messages);
    }

    private Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return jwtUtil.getUserIdFromToken(token);
        }
        throw new com.im.system.common.UnauthorizedException("未授权，请先登录");
    }
}
