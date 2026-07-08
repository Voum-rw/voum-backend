package com.voum.modules.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageDto {
    private UUID id;
    private UUID senderId;
    private String senderName;
    private UUID receiverId;
    private UUID contextId;
    private String contextType;
    private String content;
    private Instant sentAt;
    private boolean isRead;
}
