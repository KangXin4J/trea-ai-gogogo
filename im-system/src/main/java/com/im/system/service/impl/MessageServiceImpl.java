package com.im.system.service.impl;

import com.im.system.dto.SendMessageRequest;
import com.im.system.entity.Message;
import com.im.system.repository.ConversationMemberRepository;
import com.im.system.repository.ConversationRepository;
import com.im.system.repository.MessageRepository;
import com.im.system.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    @Transactional
    public Message sendMessage(Long senderId, SendMessageRequest request) {
        if (!userRepository.existsById(senderId)) {
            throw new RuntimeException("发送者不存在");
        }

        if (request.getReceiverId() != null && !userRepository.existsById(request.getReceiverId())) {
            throw new RuntimeException("接收者不存在");
        }

        if (request.getConversationId() == null) {
            throw new RuntimeException("会话ID不能为空");
        }

        if (!conversationRepository.existsById(request.getConversationId())) {
            throw new RuntimeException("会话不存在");
        }

        conversationMemberRepository.findByConversationIdAndUserId(request.getConversationId(), senderId)
                .orElseThrow(() -> new RuntimeException("无权发送消息到该会话"));

        Message message = new Message();
        message.setSenderId(senderId);
        message.setReceiverId(request.getReceiverId());
        message.setContent(request.getContent());
        message.setType(request.getType() != null ? request.getType() : "TEXT");
        message.setConversationId(request.getConversationId());
        message.setIsRead(false);

        Message savedMessage = messageRepository.save(message);

        conversationService.updateConversationLastMessage(request.getConversationId(), request.getContent());

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
