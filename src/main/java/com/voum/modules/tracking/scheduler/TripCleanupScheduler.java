package com.voum.modules.tracking.scheduler;

import com.voum.modules.tracking.repository.TripTrackingPointRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class TripCleanupScheduler {

    private static final Logger log = LoggerFactory.getLogger(TripCleanupScheduler.class);

    private final TripTrackingPointRepository tripTrackingPointRepository;

    /**
     * Purges tracking points older than 30 days.
     * Scheduled to run daily at 2:00 AM.
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupOldTrackingPoints() {
        Instant cutoff = Instant.now().minus(30, ChronoUnit.DAYS);
        log.info("Starting automated cleanup of trip tracking points older than: {}", cutoff);

        try {
            int deletedCount = tripTrackingPointRepository.deleteOlderThan(cutoff);
            log.info("Completed tracking points cleanup. Successfully deleted {} records.", deletedCount);
        } catch (Exception e) {
            log.error("Failed to run automated cleanup of trip tracking points.", e);
        }
    }
}
