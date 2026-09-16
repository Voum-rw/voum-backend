package com.voum.modules.marketplace.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/v1/activity")
@RequiredArgsConstructor
public class ActivityController {
    private final JdbcTemplate jdbc;

    @GetMapping("/peak-hours")
    @PreAuthorize("hasRole('MOTARI')")
    public Map<String, Object> peakHours() {
        var hours = jdbc.queryForList("""
            SELECT EXTRACT(HOUR FROM created_at AT TIME ZONE 'UTC' AT TIME ZONE 'Africa/Kigali')::int AS hour,
                   COUNT(*) AS requests
            FROM ride_requests
            WHERE created_at >= CURRENT_TIMESTAMP - INTERVAL '30 days'
            GROUP BY 1 ORDER BY requests DESC, hour ASC
            """);
        long total = hours.stream().mapToLong(h -> ((Number) h.get("requests")).longValue()).sum();
        return Map.of("timezone", "Africa/Kigali", "windowDays", 30,
                "sampleSize", total, "hours", total < 5 ? List.of() : hours.stream().limit(3).toList());
    }
}
