package com.undoschool.booking.scheduler;

import com.undoschool.booking.repository.IdempotentRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyCleanupScheduler {

    private final IdempotentRequestRepository repository;

    // Run every hour to purge keys older than 24 hours
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void purgeExpiredIdempotencyKeys() {
        Instant cutoff = Instant.now().minus(24, ChronoUnit.HOURS);
        log.info("Starting idempotency keys cleanup. Purging requests created before {}", cutoff);
        try {
            repository.deleteByCreatedAtBefore(cutoff);
            log.info("Idempotency keys cleanup completed successfully.");
        } catch (Exception e) {
            log.error("Failed to purge expired idempotency keys", e);
        }
    }
}
