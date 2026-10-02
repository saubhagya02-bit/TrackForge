package com.trackforge.analytics.service;

import com.trackforge.analytics.dto.AnalyticsDto;
import com.trackforge.analytics.entity.BugEventRecord;
import com.trackforge.analytics.repository.BugEventRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final BugEventRecordRepository repository;

    public AnalyticsDto.TrendResponse getTrend(UUID projectId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<Object[]> rows = repository.dailyTrend(projectId, since);

        Map<LocalDate, long[]> byDay = new TreeMap<>(); // [created, resolved]
        for (Object[] row : rows) {
            LocalDate day = ((java.sql.Date) row[0]).toLocalDate();
            String eventType = (String) row[1];
            long count = ((Number) row[2]).longValue();
            long[] bucket = byDay.computeIfAbsent(day, d -> new long[2]);
            if ("BUG_CREATED".equals(eventType)) bucket[0] += count;
            else bucket[1] += count;
        }

        List<AnalyticsDto.TrendPoint> points = new ArrayList<>();
        for (Map.Entry<LocalDate, long[]> e : byDay.entrySet()) {
            points.add(AnalyticsDto.TrendPoint.builder()
                    .date(e.getKey())
                    .created(e.getValue()[0])
                    .resolved(e.getValue()[1])
                    .build());
        }

        return AnalyticsDto.TrendResponse.builder().days(days).points(points).build();
    }

    public AnalyticsDto.ResolutionTimeResponse getResolutionTime(UUID projectId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<BugEventRecord> created = repository.findByProjectIdAndEventTypeAndOccurredAtAfter(
                projectId, "BUG_CREATED", since);
        Map<UUID, LocalDateTime> createdAt = new HashMap<>();
        for (BugEventRecord r : created) createdAt.put(r.getBugId(), r.getOccurredAt());

        List<BugEventRecord> resolutions = repository.findResolutionEvents(projectId, since);
        // Keep only the FIRST resolution per bug (list is ordered by bugId, occurredAt asc)
        Map<UUID, LocalDateTime> firstResolvedAt = new LinkedHashMap<>();
        for (BugEventRecord r : resolutions) {
            firstResolvedAt.putIfAbsent(r.getBugId(), r.getOccurredAt());
        }

        double totalHours = 0;
        long count = 0;
        for (Map.Entry<UUID, LocalDateTime> entry : firstResolvedAt.entrySet()) {
            LocalDateTime start = createdAt.get(entry.getKey());
            if (start == null) continue; // bug was created before the analytics window
            totalHours += Duration.between(start, entry.getValue()).toMinutes() / 60.0;
            count++;
        }

        return AnalyticsDto.ResolutionTimeResponse.builder()
                .days(days)
                .resolvedCount(count)
                .avgResolutionHours(count > 0 ? Math.round((totalHours / count) * 100.0) / 100.0 : 0.0)
                .build();
    }

    public AnalyticsDto.OverviewResponse getOverview(List<UUID> projectIds, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        long totalCreated = 0;
        long totalResolved = 0;
        for (UUID projectId : projectIds) {
            totalCreated += repository.countByProjectIdAndEventTypeAndOccurredAtAfter(
                    projectId, "BUG_CREATED", since);
            totalResolved += repository.findResolutionEvents(projectId, since).stream()
                    .map(BugEventRecord::getBugId).distinct().count();
        }
        return AnalyticsDto.OverviewResponse.builder()
                .days(days).totalCreated(totalCreated).totalResolved(totalResolved).build();
    }
}