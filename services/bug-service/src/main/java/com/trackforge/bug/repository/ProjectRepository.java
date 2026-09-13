package com.trackforge.bug.repository;

import com.trackforge.bug.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectRepository extends JpaRepository<Project, UUID> {
    Optional<Project> findByKey(String key);
    boolean existsByKey(String key);
    List<Project> findByOwnerId(UUID ownerId);
    List<Project> findByStatus(Project.Status status);

    @Modifying
    @Query("UPDATE Project p SET p.bugCounter = p.bugCounter + 1 WHERE p.id = :id")
    void incrementBugCounter(UUID id);
}