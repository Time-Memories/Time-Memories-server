package com.example.memories.domain.chat.repository;

import com.example.memories.domain.chat.entity.Chat;
import com.example.memories.domain.chat.entity.ChatImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatImageRepository extends JpaRepository<ChatImage, Long> {
    List<ChatImage> findByChatInOrderBySortOrderAsc(List<Chat> chats);
}
