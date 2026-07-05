package com.im.system.repository;

import com.im.system.entity.ConversationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationMemberRepository extends JpaRepository<ConversationMember, Long> {

    List<ConversationMember> findByUserId(Long userId);

    List<ConversationMember> findByConversationId(Long conversationId);

    Optional<ConversationMember> findByConversationIdAndUserId(Long conversationId, Long userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM ConversationMember cm WHERE cm.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") Long conversationId);
}
