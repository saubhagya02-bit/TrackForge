package com.trackforge.analytics.repository;

import com.trackforge.analytics.entity.BugEventRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface BugEventRecordRepository extends JpaRepository<BugEventRecord, UUID> {

    List<BugEventRecord> findByProjectIdAndEventTypeAndOccurredAtAfter(
            UUID projectId, String eventType, LocalDateTime since);

    @Query("SELECT r FROM BugEventRecord r WHERE r.projectId = :projectId " +
            "AND r.eventType = 'BUG_STATUS_CHANGED' " +
            "AND r.newStatus IN ('RESOLVED', 'CLOSED') " +
            "AND r.occurredAt >= :since " +
            "ORDER BY r.bugId, r.occurredAt ASC")
    List<BugEventRecord> findResolutionEvents(@Param("projectId") UUID projectId,
                                              @Param("since") LocalDateTime since);

    // Daily created/resolved counts for a project, used for the trend chart.
    @Query(value = "SELECT CAST(occurred_at AS date) AS day, event_type, COUNT(*) AS cnt " +
            "FROM bug_event_records " +
            "WHERE project_id = :projectId AND occurred_at >= :since " +
            "AND (event_type = 'BUG_CREATED' " +
            "     OR (event_type = 'BUG_STATUS_CHANGED' AND new_status IN ('RESOLVED','CLOSED'))) " +
            "GROUP BY day, event_type " +
            "ORDER BY day ASC", nativeQuery = true)
    List<Object[]> dailyTrend(@Param("projectId") UUID projectId, @Param("since") LocalDateTime since);

    long countByProjectIdAndEventTypeAndOccurredAtAfter(UUID projectId, String eventType, LocalDateTime since);
}