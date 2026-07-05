package com.im.system.service.impl;

import com.im.system.dto.CreateConversationRequest;
import com.im.system.entity.Conversation;
import com.im.system.entity.ConversationMember;
import com.im.system.repository.ConversationMemberRepository;
import com.im.system.repository.ConversationRepository;
import com.im.system.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;

    @Override
    public List<Conversation> getUserConversations(Long userId) {
        return conversationRepository.findByUserId(userId);
    }

    @Override
    public Conversation getConversationById(Long userId, Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("无权访问该会话"));

        return conversation;
    }

    @Override
    @Transactional
    public Conversation createConversation(Long userId, CreateConversationRequest request) {
        Conversation conversation = new Conversation();
        conversation.setType(request.getType());
        conversation.setName(request.getName());
        conversation.setLastMessage(null);
        conversation.setLastMessageTime(null);

        Conversation savedConversation = conversationRepository.save(conversation);

        ConversationMember creatorMember = new ConversationMember();
        creatorMember.setConversationId(savedConversation.getId());
        creatorMember.setUserId(userId);
        conversationMemberRepository.save(creatorMember);

        for (Long memberId : request.getMemberIds()) {
            if (!memberId.equals(userId)) {
                ConversationMember member = new ConversationMember();
                member.setConversationId(savedConversation.getId());
                member.setUserId(memberId);
                conversationMemberRepository.save(member);
            }
        }

        return savedConversation;
    }

    @Override
    @Transactional
    public void deleteConversation(Long userId, Long conversationId) {
        conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("无权删除该会话"));

        conversationMemberRepository.deleteByConversationId(conversationId);
        conversationRepository.deleteById(conversationId);
    }

    @Override
    @Transactional
    public void updateConversationLastMessage(Long conversationId, String content) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
        conversation.setLastMessage(content);
        conversation.setLastMessageTime(LocalDateTime.now());
        conversationRepository.save(conversation);
    }
}