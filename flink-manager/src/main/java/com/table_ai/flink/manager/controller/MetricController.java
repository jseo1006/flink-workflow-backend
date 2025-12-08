package com.table_ai.flink.manager.controller;

import com.table_ai.flink.manager.service.MetricService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricController {

    private final MetricService metricService;

    public MetricController(MetricService metricService) {
        this.metricService = metricService;
    }

    @GetMapping("/{appName}")
    public Map<String, Double> getMetrics(@PathVariable String appName) {
        return metricService.getJobMetrics(appName);
    }
}
