package com.im.system.service;

import com.im.system.dto.ConversationMemberDTO;
import com.im.system.dto.CreateConversationRequest;
import com.im.system.entity.Conversation;

import java.util.List;

public interface ConversationService {

    List<Conversation> getUserConversations(Long userId);

    Conversation getConversationById(Long userId, Long conversationId);

    Conversation createConversation(Long userId, CreateConversationRequest request);

    void deleteConversation(Long userId, Long conversationId);

    void updateConversationLastMessage(Long conversationId, String content);

    List<ConversationMemberDTO> getConversationMembers(Long userId, Long conversationId);
}