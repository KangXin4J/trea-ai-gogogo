package com.im.system.service;

import com.im.system.dto.PageResponse;
import com.im.system.dto.SendMessageRequest;
import com.im.system.entity.Message;

import java.util.List;

public interface MessageService {

    Message sendMessage(Long senderId, SendMessageRequest request);

    List<Message> getConversationMessages(Long userId1, Long userId2);

    List<Message> getMessagesByConversationId(Long conversationId);

    PageResponse<Message> getMessagesByConversationIdPaged(Long userId, Long conversationId, int page, int size);

    PageResponse<Message> searchMessages(Long userId, Long conversationId, String keyword, int page, int size);

    long getUnreadCount(Long userId);

    void markAsRead(Long userId, Long conversationId);

    void deleteMessage(Long messageId, Long userId);

    void recallMessage(Long messageId, Long userId);
}
