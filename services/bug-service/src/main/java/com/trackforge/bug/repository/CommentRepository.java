package com.trackforge.bug.repository;

import com.trackforge.bug.entity.ActivityLog;
import com.trackforge.bug.entity.Attachment;
import com.trackforge.bug.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByBugIdOrderByCreatedAtAsc(UUID bugId);
    long countByBugId(UUID bugId);
}

