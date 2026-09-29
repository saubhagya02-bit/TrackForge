package com.trackforge.bug.service;

import com.trackforge.bug.dto.BugDto;
import com.trackforge.bug.dto.ProjectDto;
import com.trackforge.bug.entity.Bug;
import com.trackforge.bug.entity.Project;
import com.trackforge.bug.repository.BugRepository;
import com.trackforge.bug.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Logger log =
            LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final BugRepository bugRepository;
    private final StorageService storageService;

    private static final Pattern KEY_PATTERN =
            Pattern.compile("^[A-Z0-9]{2,20}$");


    public List<ProjectDto.Response> getAll() {

        return projectRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get a project by ID.
     */
    public ProjectDto.Response getById(UUID id) {
        return toResponse(findOrThrow(id));
    }

    /**
     * Create a new project.
     */
    @Transactional
    @CacheEvict(
            value = {"projects", "bugs", "bugStats"},
            allEntries = true
    )
    public ProjectDto.Response create(
            ProjectDto.CreateRequest req,
            UUID ownerId
    ) {

        if (req == null) {
            throw new IllegalArgumentException(
                    "Project request cannot be null"
            );
        }

        if (req.getName() == null || req.getName().isBlank()) {
            throw new IllegalArgumentException(
                    "Project name is required"
            );
        }

        if (req.getKey() == null || req.getKey().isBlank()) {
            throw new IllegalArgumentException(
                    "Project key is required"
            );
        }

        String key = req.getKey()
                .trim()
                .toUpperCase();

        if (!KEY_PATTERN.matcher(key).matches()) {
            throw new IllegalArgumentException(
                    "Project key must be 2-20 uppercase letters/digits"
            );
        }

        if (projectRepository.existsByKey(key)) {
            throw new IllegalArgumentException(
                    "Project key already in use: " + key
            );
        }

        Project project = Project.builder()
                .name(req.getName().trim())
                .description(req.getDescription())
                .key(key)
                .status(Project.Status.ACTIVE)
                .ownerId(ownerId)
                .bugCounter(0)
                .build();

        Project saved = projectRepository.save(project);

        log.info(
                "Project created: {} ({})",
                saved.getName(),
                saved.getKey()
        );

        return toResponse(saved);
    }

    /**
     * Update an existing project.
     */
    @Transactional
    @CacheEvict(
            value = {"projects", "bugs", "bugStats"},
            allEntries = true
    )
    public ProjectDto.Response update(
            UUID id,
            ProjectDto.UpdateRequest req
    ) {

        if (req == null) {
            throw new IllegalArgumentException(
                    "Update request cannot be null"
            );
        }

        Project project = findOrThrow(id);

        if (req.getName() != null &&
                !req.getName().isBlank()) {

            project.setName(req.getName().trim());
        }

        if (req.getDescription() != null) {
            project.setDescription(req.getDescription());
        }

        if (req.getStatus() != null &&
                !req.getStatus().isBlank()) {

            try {
                project.setStatus(
                        Project.Status.valueOf(
                                req.getStatus()
                                        .trim()
                                        .toUpperCase()
                        )
                );
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(
                        "Invalid project status: " +
                                req.getStatus()
                );
            }
        }

        Project updated = projectRepository.save(project);

        log.info(
                "Project updated: {} ({})",
                updated.getName(),
                updated.getKey()
        );

        return toResponse(updated);
    }

    /**
     * Delete a project.
     */
    @Transactional
    @CacheEvict(
            value = {"projects", "bugs", "bugStats"},
            allEntries = true
    )
    public void delete(UUID id) {

        Project project = findOrThrow(id);

        if (project.getBugs() != null) {

            project.getBugs().forEach(bug -> {

                if (bug.getAttachments() != null) {

                    bug.getAttachments().forEach(
                            attachment -> {

                                try {
                                    storageService.delete(
                                            attachment
                                                    .getStoredFilename()
                                    );
                                } catch (Exception e) {

                                    log.warn(
                                            "Failed to delete attachment: {}",
                                            attachment
                                                    .getStoredFilename(),
                                            e
                                    );
                                }
                            }
                    );
                }
            });
        }

        projectRepository.delete(project);

        log.info(
                "Project deleted: {}",
                project.getKey()
        );
    }

    // Helpers

    private Project findOrThrow(UUID id) {

        if (id == null) {
            throw new IllegalArgumentException(
                    "Project ID cannot be null"
            );
        }

        return projectRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Project not found: " + id
                        )
                );
    }

    /**
     * Convert Project entity to API response.
     */
    private ProjectDto.Response toResponse(Project project) {

        long total =
                bugRepository.countByProjectId(
                        project.getId()
                );

        long open =
                bugRepository.countByProjectIdAndStatus(
                        project.getId(),
                        Bug.Status.OPEN
                )
                        +
                        bugRepository.countByProjectIdAndStatus(
                                project.getId(),
                                Bug.Status.REOPENED
                        );

        long inProgress =
                bugRepository.countByProjectIdAndStatus(
                        project.getId(),
                        Bug.Status.IN_PROGRESS
                )
                        +
                        bugRepository.countByProjectIdAndStatus(
                                project.getId(),
                                Bug.Status.IN_REVIEW
                        );

        long resolved =
                bugRepository.countByProjectIdAndStatus(
                        project.getId(),
                        Bug.Status.RESOLVED
                )
                        +
                        bugRepository.countByProjectIdAndStatus(
                                project.getId(),
                                Bug.Status.CLOSED
                        );

        return ProjectDto.Response.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .key(project.getKey())
                .status(project.getStatus() != null
                        ? project.getStatus().name()
                        : null)
                .owner(
                        BugDto.UserRef.builder()
                                .id(project.getOwnerId())
                                .build()
                )
                .totalBugs(total)
                .openBugs(open)
                .inProgressBugs(inProgress)
                .resolvedBugs(resolved)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }
}