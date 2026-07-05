package com.im.system.service.impl;

import com.im.system.dto.SendMessageRequest;
import com.im.system.entity.Message;
import com.im.system.repository.MessageRepository;
import com.im.system.service.ConversationService;
import com.im.system.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;

    @Override
    @Transactional
    public Message sendMessage(Long senderId, SendMessageRequest request) {
        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(request.getReceiverId());
        message.setContent(request.getContent());
        message.setType(request.getType() != null ? request.getType() : "TEXT");
        message.setConversationId(request.getConversationId());
        message.setIsRead(false);

        Message savedMessage = messageRepository.save(message);

        if (request.getConversationId() != null) {
            conversationService.updateConversationLastMessage(request.getConversationId(), request.getContent());
        }

        return savedMessage;
    }

    @Override
    public List<Message> getConversationMessages(Long userId1, Long userId2) {
        return messageRepository.findConversationMessages(userId1, userId2);
    }

    @Override
    public List<Message> getMessagesByConversationId(Long conversationId) {
        return messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    public long getUnreadCount(Long userId) {
        return messageRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    @Override
    @Transactional
    public void markAsRead(Long userId, Long conversationId) {
        List<Message> messages;
        if (conversationId != null) {
            messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        } else {
            messages = messageRepository.findAll();
        }
        messages.stream()
                .filter(m -> m.getReceiverId().equals(userId) && !m.getIsRead())
                .forEach(m -> m.setIsRead(true));
        messageRepository.saveAll(messages);
    }
}
