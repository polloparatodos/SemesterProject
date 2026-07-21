package com.agenthub.chat.repository;

import com.agenthub.chat.entity.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    List<ChatMessage> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    List<ChatMessage> findBySessionId(String sessionId);

    List<ChatMessage> findBySessionIdAndCreatedAtBetween(
        String sessionId,
        LocalDateTime startTime,
        LocalDateTime endTime
    );

    List<ChatMessage> findBySessionIdAndSender(String sessionId, String sender);

    long countBySessionId(String sessionId);

    void deleteBySessionId(String sessionId);
}
