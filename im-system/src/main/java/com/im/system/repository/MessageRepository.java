package com.im.system.repository;

import com.im.system.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId);

    @Query("SELECT m FROM Message m WHERE (m.senderId = :userId1 AND m.receiverId = :userId2) OR (m.senderId = :userId2 AND m.receiverId = :userId1) ORDER BY m.createdAt ASC")
    List<Message> findConversationMessages(@Param("userId1") Long userId1, @Param("userId2") Long userId2);

    long countByReceiverIdAndIsReadFalse(Long receiverId);

    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);

    @Query("SELECT m FROM Message m WHERE m.conversationId = :conversationId AND m.content LIKE %:keyword% ORDER BY m.createdAt DESC")
    Page<Message> searchByConversationIdAndContent(@Param("conversationId") Long conversationId, @Param("keyword") String keyword, Pageable pageable);
}
