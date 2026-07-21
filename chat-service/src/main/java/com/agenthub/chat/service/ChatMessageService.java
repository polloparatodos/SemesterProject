package com.agenthub.chat.service;

import com.agenthub.chat.entity.ChatMessage;
import com.agenthub.chat.repository.ChatMessageRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChatMessageService {

    private final ChatMessageRepository chatMessageRepository;

    public ChatMessageService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    public ChatMessage save(ChatMessage message) {
        return chatMessageRepository.save(message);
    }

    public List<ChatMessage> getMessagesBySessionId(String sessionId) {
        return chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }

    public long countBySessionId(String sessionId) {
        return chatMessageRepository.countBySessionId(sessionId);
    }

    public void deleteBySessionId(String sessionId) {
        chatMessageRepository.deleteBySessionId(sessionId);
    }
}
