package com.im.system.websocket;

import com.im.system.dto.SendMessageRequest;
import com.im.system.entity.Message;
import com.im.system.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketHandler {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendMessageRequest request, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());
        log.debug("收到消息: senderId={}, receiverId={}, content={}", senderId, request.getReceiverId(), request.getContent());

        Message message = messageService.sendMessage(senderId, request);

        messagingTemplate.convertAndSendToUser(
                String.valueOf(request.getReceiverId()),
                "/queue/messages",
                message
        );

        messagingTemplate.convertAndSendToUser(
                String.valueOf(senderId),
                "/queue/messages",
                message
        );
    }

    @MessageMapping("/chat.ping")
    @SendToUser("/queue/pong")
    public String ping(String message) {
        return "pong: " + message;
    }

    @MessageMapping("/chat.broadcast")
    @SendTo("/topic/public")
    public Message broadcastMessage(@Payload SendMessageRequest request, Principal principal) {
        Long senderId = Long.valueOf(principal.getName());
        request.setReceiverId(0L);
        return messageService.sendMessage(senderId, request);
    }
}
