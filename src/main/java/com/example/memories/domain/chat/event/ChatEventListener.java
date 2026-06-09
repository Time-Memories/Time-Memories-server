package com.example.memories.domain.chat.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ChatEventListener {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatCreated(ChatCreatedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + event.roomId(),
                event.response()
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleChatDeleted(ChatDeletedEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + event.roomId() + "/updates",
                event.response()
        );
    }

}
