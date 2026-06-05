package com.example.memories.infra.s3;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class S3ImageDeleteEventListener {

    private final S3PresignService s3PresignService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleImageDelete(S3ImageDeleteEvent event) {
        event.imageKeys().forEach(s3PresignService::delete);
    }
}
