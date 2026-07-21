package com.agenthub.chat.controller;

import com.agenthub.chat.entity.ChatMessage;
import com.agenthub.chat.service.ChatMessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class ChatMessageController {

    private final ChatMessageService chatMessageService;

    public ChatMessageController(ChatMessageService chatMessageService) {
        this.chatMessageService = chatMessageService;
    }

    @PostMapping
    public ResponseEntity<ChatMessage> createMessage(@RequestBody ChatMessage message) {
        ChatMessage saved = chatMessageService.save(message);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<List<ChatMessage>> getMessagesBySessionId(@PathVariable("sessionId") String sessionId) {
        List<ChatMessage> messages = chatMessageService.getMessagesBySessionId(sessionId);
        return ResponseEntity.ok(messages);
    }

    @GetMapping("/session/{sessionId}/count")
    public ResponseEntity<Long> countMessages(@PathVariable("sessionId") String sessionId) {
        long count = chatMessageService.countBySessionId(sessionId);
        return ResponseEntity.ok(count);
    }

    @DeleteMapping("/session/{sessionId}")
    public ResponseEntity<Void> deleteMessages(@PathVariable("sessionId") String sessionId) {
        chatMessageService.deleteBySessionId(sessionId);
        return ResponseEntity.noContent().build();
    }
}
