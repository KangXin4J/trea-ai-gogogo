package com.im.system.service.impl;

import com.im.system.dto.AddMembersRequest;
import com.im.system.dto.ConversationMemberDTO;
import com.im.system.dto.CreateConversationRequest;
import com.im.system.dto.PageResponse;
import com.im.system.dto.UpdateConversationRequest;
import com.im.system.entity.Conversation;
import com.im.system.entity.ConversationMember;
import com.im.system.repository.ConversationMemberRepository;
import com.im.system.repository.ConversationRepository;
import com.im.system.repository.UserRepository;
import com.im.system.service.ConversationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ConversationServiceImpl implements ConversationService {

    private final ConversationRepository conversationRepository;
    private final ConversationMemberRepository conversationMemberRepository;
    private final UserRepository userRepository;

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
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("无权删除该会话"));

        conversationMemberRepository.deleteByConversationId(conversationId);
        conversationRepository.deleteById(conversationId);
    }

    @Override
    @Transactional
    public Conversation updateConversation(Long userId, Long conversationId, UpdateConversationRequest request) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("无权更新该会话"));

        if (request.getName() != null) {
            conversation.setName(request.getName());
        }
        if (request.getType() != null) {
            conversation.setType(request.getType());
        }

        return conversationRepository.save(conversation);
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

    @Override
    public List<ConversationMemberDTO> getConversationMembers(Long userId, Long conversationId) {
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("无权访问该会话"));

        return conversationMemberRepository.findMembersByConversationId(conversationId);
    }

    @Override
    @Transactional
    public List<ConversationMemberDTO> addMembers(Long userId, Long conversationId, AddMembersRequest request) {
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("无权访问该会话"));

        for (Long memberId : request.getMemberIds()) {
            if (!memberId.equals(userId)) {
                if (!userRepository.existsById(memberId)) {
                    throw new RuntimeException("用户不存在");
                }

                conversationMemberRepository.findByConversationIdAndUserId(conversationId, memberId)
                        .ifPresent(member -> {
                            throw new RuntimeException("用户已在会话中");
                        });

                ConversationMember member = new ConversationMember();
                member.setConversationId(conversationId);
                member.setUserId(memberId);
                conversationMemberRepository.save(member);
            }
        }

        return conversationMemberRepository.findMembersByConversationId(conversationId);
    }

    @Override
    @Transactional
    public List<ConversationMemberDTO> removeMember(Long userId, Long conversationId, Long memberId) {
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("无权访问该会话"));

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, memberId)
                .orElseThrow(() -> new RuntimeException("用户不是会话成员"));

        conversationMemberRepository.delete(member);

        return conversationMemberRepository.findMembersByConversationId(conversationId);
    }

    @Override
    @Transactional
    public void leaveConversation(Long userId, Long conversationId) {
        conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));

        ConversationMember member = conversationMemberRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new RuntimeException("用户不是会话成员"));

        conversationMemberRepository.delete(member);
    }

    @Override
    public PageResponse<Conversation> searchConversations(Long userId, String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Conversation> conversationPage = conversationRepository.searchByUserIdAndName(userId, keyword, pageable);
        return new PageResponse<>(conversationPage.getContent(), page, size, conversationPage.getTotalElements());
    }
}