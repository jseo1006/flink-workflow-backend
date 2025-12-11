package com.table_ai.flink.manager.controller;

import com.table_ai.flink.manager.service.CloudWatchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    private final CloudWatchService cloudWatchService;

    public MetricController(CloudWatchService cloudWatchService) {
        this.cloudWatchService = cloudWatchService;
    }

    @GetMapping
    public Map<String, Object> getMetrics(@RequestParam String applicationName) {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("applicationName", applicationName);
        metrics.put("throughput", cloudWatchService.getThroughput(applicationName));
        metrics.put("latency", cloudWatchService.getLatency(applicationName));
        return metrics;
    }
}
