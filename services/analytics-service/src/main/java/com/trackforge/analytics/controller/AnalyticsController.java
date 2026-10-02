package com.trackforge.analytics.controller;

import com.trackforge.analytics.dto.AnalyticsDto;
import com.trackforge.analytics.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Bug trend and resolution-time analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/projects/{projectId}/trend")
    @Operation(summary = "Daily created vs. resolved bug counts for a project")
    public ResponseEntity<AnalyticsDto.TrendResponse> getTrend(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "14") int days) {
        return ResponseEntity.ok(analyticsService.getTrend(projectId, days));
    }

    @GetMapping("/projects/{projectId}/resolution-time")
    @Operation(summary = "Average time-to-resolution for a project")
    public ResponseEntity<AnalyticsDto.ResolutionTimeResponse> getResolutionTime(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getResolutionTime(projectId, days));
    }

    @GetMapping("/overview")
    @Operation(summary = "Aggregate created/resolved totals across a set of projects")
    public ResponseEntity<AnalyticsDto.OverviewResponse> getOverview(
            @RequestParam List<UUID> projectIds,
            @RequestParam(defaultValue = "30") int days) {
        return ResponseEntity.ok(analyticsService.getOverview(projectIds, days));
    }
}