package org.example.chessmystic.Models.KafkaEvents;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceMetricsEvent {
    private String eventId;
    private String gameId;
    private String playerId;
    private long timestamp;

    // Performance data
    private double cpuUsage;
    private long memoryUsage;
    private long networkLatency;
    private int frameRate;
    private long renderTime;
    private long inputLag;

}