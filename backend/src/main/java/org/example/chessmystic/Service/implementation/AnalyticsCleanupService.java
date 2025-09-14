package org.example.chessmystic.Service.implementation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AnalyticsCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(AnalyticsCleanupService.class);

    @Autowired
    private AnalyticsService analyticsService;

    // Run cleanup every hour
    @Scheduled(fixedRate = 3600000) // 1 hour in milliseconds
    public void cleanupOrphanedGames() {
        try {
            logger.info("Starting analytics cleanup task...");
            analyticsService.cleanupOrphanedGames();
            logger.info("Analytics cleanup task completed successfully");
        } catch (Exception e) {
            logger.error("Error during analytics cleanup task: {}", e.getMessage(), e);
        }
    }
}
