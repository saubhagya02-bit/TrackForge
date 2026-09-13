package com.trackforge.bug.repository;

import com.trackforge.bug.entity.Bug;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BugRepository extends JpaRepository<Bug, UUID> {

    Page<Bug> findByProjectId(UUID projectId, Pageable pageable);

    @Query("SELECT b FROM Bug b WHERE b.project.id = :projectId " +
            "AND (:status IS NULL OR b.status = :status) " +
            "AND (:priority IS NULL OR b.priority = :priority) " +
            "AND (:assigneeId IS NULL OR b.assigneeId = :assigneeId)")
    Page<Bug> findByProjectIdWithFilters(
            @Param("projectId") UUID projectId,
            @Param("status") Bug.Status status,
            @Param("priority") Bug.Priority priority,
            @Param("assigneeId") UUID assigneeId,
            Pageable pageable
    );

    Page<Bug> findByAssigneeId(UUID assigneeId, Pageable pageable);
    Page<Bug> findByReporterId(UUID reporterId, Pageable pageable);

    Optional<Bug> findByBugNumber(String bugNumber);
    long countByProjectId(UUID projectId);
    long countByProjectIdAndStatus(UUID projectId, Bug.Status status);

    @Query("SELECT b.status, COUNT(b) FROM Bug b WHERE b.project.id = :projectId GROUP BY b.status")
    List<Object[]> countGroupByStatus(@Param("projectId") UUID projectId);

    @Query("SELECT b.priority, COUNT(b) FROM Bug b WHERE b.project.id = :projectId GROUP BY b.priority")
    List<Object[]> countGroupByPriority(@Param("projectId") UUID projectId);

    // Full-text search using PostgreSQL
    @Query(value = "SELECT * FROM bugs WHERE project_id = :projectId " +
            "AND to_tsvector('english', title || ' ' || COALESCE(description, '')) " +
            "@@ plainto_tsquery('english', :query)", nativeQuery = true)
    List<Bug> fullTextSearch(@Param("projectId") UUID projectId, @Param("query") String query);

    // Semantic duplicate detection using pgvector cosine similarity
    @Query(value = "SELECT * FROM bugs WHERE project_id = :projectId " +
            "AND id != :excludeId " +
            "AND embedding IS NOT NULL " +
            "AND 1 - (embedding <=> CAST(:embedding AS vector)) > :threshold " +
            "ORDER BY embedding <=> CAST(:embedding AS vector) LIMIT 5",
            nativeQuery = true)
    List<Bug> findSimilarBugs(
            @Param("projectId") UUID projectId,
            @Param("excludeId") UUID excludeId,
            @Param("embedding") String embedding,
            @Param("threshold") float threshold
    );
}