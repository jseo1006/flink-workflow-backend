package com.table_ai.flink.manager.service;

import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
public class MetricService {

    private final CloudWatchClient cloudWatchClient;

    public MetricService(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    public Map<String, Double> getJobMetrics(String appName) {
        Map<String, Double> metrics = new HashMap<>();

        // Example: Fetch "records-out-per-second" (Throughput)
        // CloudWatch Metric Name for Kinesis Analytics: "numRecordsOutPerSecond"

        metrics.put("throughput", getMetricValue(appName, "numRecordsOutPerSecond"));
        metrics.put("latency", getMetricValue(appName, "millisBehindLatest")); // Proxy for latency

        return metrics;
    }

    private Double getMetricValue(String appName, String metricName) {
        try {
            GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                    .namespace("AWS/KinesisAnalytics")
                    .metricName(metricName)
                    .dimensions(Dimension.builder().name("Application").value(appName).build())
                    .startTime(Instant.now().minus(5, ChronoUnit.MINUTES))
                    .endTime(Instant.now())
                    .period(60) // 1 minute granularity
                    .statistics(Statistic.AVERAGE)
                    .build();

            GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);

            if (!response.datapoints().isEmpty()) {
                // Return the latest average
                return response.datapoints().get(0).average();
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch metric " + metricName + ": " + e.getMessage());
        }
        return 0.0;
    }
}
