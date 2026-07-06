package com.im.system.service;

import com.im.system.dto.AddMembersRequest;
import com.im.system.dto.ConversationMemberDTO;
import com.im.system.dto.CreateConversationRequest;
import com.im.system.dto.PageResponse;
import com.im.system.dto.UpdateConversationRequest;
import com.im.system.entity.Conversation;

import java.util.List;

public interface ConversationService {

    List<Conversation> getUserConversations(Long userId);

    Conversation getConversationById(Long userId, Long conversationId);

    Conversation createConversation(Long userId, CreateConversationRequest request);

    void deleteConversation(Long userId, Long conversationId);

    Conversation updateConversation(Long userId, Long conversationId, UpdateConversationRequest request);

    void updateConversationLastMessage(Long conversationId, String content);

    List<ConversationMemberDTO> getConversationMembers(Long userId, Long conversationId);

    List<ConversationMemberDTO> addMembers(Long userId, Long conversationId, AddMembersRequest request);

    List<ConversationMemberDTO> removeMember(Long userId, Long conversationId, Long memberId);

    PageResponse<Conversation> searchConversations(Long userId, String keyword, int page, int size);
}