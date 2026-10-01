package com.trackforge.analytics.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

public class AnalyticsDto {

    @Data @Builder
    public static class TrendPoint {
        private LocalDate date;
        private long created;
        private long resolved;
    }

    @Data @Builder
    public static class TrendResponse {
        private int days;
        private List<TrendPoint> points;
    }

    @Data @Builder
    public static class ResolutionTimeResponse {
        private int days;
        private long resolvedCount;
        private double avgResolutionHours;
    }

    @Data @Builder
    public static class OverviewResponse {
        private int days;
        private long totalCreated;
        private long totalResolved;
    }
}