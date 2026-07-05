package com.im.system.repository;

import com.im.system.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT c FROM Conversation c WHERE c.id IN (SELECT cm.conversationId FROM ConversationMember cm WHERE cm.userId = :userId) ORDER BY c.lastMessageTime DESC")
    List<Conversation> findByUserId(@Param("userId") Long userId);
}
