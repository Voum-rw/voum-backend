package com.voum.modules.chat;

import com.voum.common.ApiException;
import com.voum.modules.users.User;
import com.voum.modules.users.UserRepository;
import com.voum.modules.notification.service.PushNotificationService;
import com.voum.modules.notification.templates.NotificationTemplate;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final PushNotificationService pushNotificationService;

    // ──────────────────────────────────────────────────────────────────────────
    // SEND MESSAGE
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public ChatMessageDto sendMessage(UUID senderId, UUID contextId, SendMessageRequest req) {
        // Verify sender exists
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ApiException("Sender not found.", HttpStatus.NOT_FOUND));

        // Verify receiver exists
        userRepository.findById(req.getReceiverId())
                .orElseThrow(() -> new ApiException("Receiver not found.", HttpStatus.NOT_FOUND));

        ChatMessage message = ChatMessage.builder()
                .senderId(senderId)
                .receiverId(req.getReceiverId())
                .contextId(contextId)
                .contextType(req.getContextType() != null ? req.getContextType() : "REQUEST")
                .content(req.getContent().trim())
                .build();

        message = chatMessageRepository.save(message);

        ChatMessageDto dto = toDto(message, sender.getName());

        // Broadcast to both participants via WebSocket
        messagingTemplate.convertAndSend("/topic/chat/" + contextId, dto);

        // Send push notification to receiver
        try {
            pushNotificationService.sendPush(
                    req.getReceiverId(),
                    NotificationTemplate.NEW_CHAT_MESSAGE,
                    java.util.Map.of(
                            "contextId", contextId.toString(),
                            "contextType", message.getContextType(),
                            "senderName", sender.getName()
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to dispatch push notification for chat message: {}", e.getMessage());
        }

        log.info("Chat message sent in context {} by {}", contextId, senderId);
        return dto;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET HISTORY
    // ──────────────────────────────────────────────────────────────────────────

    @Transactional
    public List<ChatMessageDto> getHistory(UUID contextId, UUID callerId) {
        List<ChatMessage> messages = chatMessageRepository.findByContextIdOrderBySentAtAsc(contextId);

        // Validate caller is a participant in this conversation
        boolean isParticipant = messages.stream().anyMatch(m ->
                m.getSenderId().equals(callerId) || m.getReceiverId().equals(callerId));

        if (!messages.isEmpty() && !isParticipant) {
            throw new ApiException("Access denied to this conversation.", HttpStatus.FORBIDDEN);
        }

        // Mark unread messages as read for caller
        chatMessageRepository.markAsRead(contextId, callerId);

        return messages.stream()
                .map(m -> {
                    User sender = userRepository.findById(m.getSenderId()).orElse(null);
                    return toDto(m, sender != null ? sender.getName() : "Unknown");
                })
                .collect(Collectors.toList());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // HELPERS
    // ──────────────────────────────────────────────────────────────────────────

    private ChatMessageDto toDto(ChatMessage m, String senderName) {
        return ChatMessageDto.builder()
                .id(m.getId())
                .senderId(m.getSenderId())
                .senderName(senderName)
                .receiverId(m.getReceiverId())
                .contextId(m.getContextId())
                .contextType(m.getContextType())
                .content(m.getContent())
                .sentAt(m.getSentAt())
                .isRead(m.getIsRead())
                .build();
    }
}
