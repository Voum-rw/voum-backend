package com.voum.modules.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findByContextIdOrderBySentAtAsc(UUID contextId);

    @Modifying
    @Query("UPDATE ChatMessage m SET m.isRead = true " +
           "WHERE m.contextId = :contextId AND m.receiverId = :readerId AND m.isRead = false")
    int markAsRead(@Param("contextId") UUID contextId, @Param("readerId") UUID readerId);

    boolean existsByContextIdAndSenderIdOrContextIdAndReceiverId(
            UUID contextId1, UUID senderId, UUID contextId2, UUID receiverId);
}
