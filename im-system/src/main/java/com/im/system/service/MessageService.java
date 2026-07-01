package com.im.system.service;

import com.im.system.dto.SendMessageRequest;
import com.im.system.entity.Message;

import java.util.List;

public interface MessageService {

    Message sendMessage(Long senderId, SendMessageRequest request);

    List<Message> getConversationMessages(Long userId1, Long userId2);

    List<Message> getMessagesByConversationId(Long conversationId);

    long getUnreadCount(Long userId);

    void markAsRead(Long userId, Long conversationId);
}
