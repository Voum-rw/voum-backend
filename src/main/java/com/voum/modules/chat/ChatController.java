package com.voum.modules.chat;

import com.voum.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /** Retrieve full message history for a conversation (ride request or trip). */
    @GetMapping("/{contextId}")
    public ResponseEntity<ApiResponse<List<ChatMessageDto>>> getHistory(
            @PathVariable UUID contextId,
            @AuthenticationPrincipal String callerId) {
        List<ChatMessageDto> history = chatService.getHistory(contextId, UUID.fromString(callerId));
        return ResponseEntity.ok(ApiResponse.success(history, "Chat history retrieved."));
    }

    /** Send a message in a conversation. Also broadcasts via WebSocket. */
    @PostMapping("/{contextId}/send")
    public ResponseEntity<ApiResponse<ChatMessageDto>> sendMessage(
            @PathVariable UUID contextId,
            @Valid @RequestBody SendMessageRequest req,
            @AuthenticationPrincipal String senderId) {
        ChatMessageDto dto = chatService.sendMessage(UUID.fromString(senderId), contextId, req);
        return ResponseEntity.ok(ApiResponse.success(dto, "Message sent."));
    }
}
