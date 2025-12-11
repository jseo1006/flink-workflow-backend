package com.table_ai.flink.manager.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.cloudwatch.model.Datapoint;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetMetricStatisticsResponse;
import software.amazon.awssdk.services.cloudwatch.model.Statistic;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
public class CloudWatchService {

    private final CloudWatchClient cloudWatchClient;

    public CloudWatchService(CloudWatchClient cloudWatchClient) {
        this.cloudWatchClient = cloudWatchClient;
    }

    public Double getThroughput(String applicationName) {
        // RecordsOut per minute (Sum over last 5 minutes)
        // Note: Ideally, we divide by period to get TPS, but Sum per minute is also
        // fine for visual.
        return getMetric(applicationName, "RecordsOut", Statistic.SUM);
    }

    public Double getLatency(String applicationName) {
        // MillisBehindLatest (Max over last 5 minutes)
        return getMetric(applicationName, "MillisBehindLatest", Statistic.MAXIMUM);
    }

    private Double getMetric(String applicationName, String metricName, Statistic statistic) {
        Instant endTime = Instant.now();
        Instant startTime = endTime.minus(5, ChronoUnit.MINUTES);

        GetMetricStatisticsRequest request = GetMetricStatisticsRequest.builder()
                .namespace("AWS/KinesisAnalytics")
                .metricName(metricName)
                .dimensions(Dimension.builder().name("Application").value(applicationName).build())
                .startTime(startTime)
                .endTime(endTime)
                .period(60) // 1 minute datapoints
                .statistics(statistic)
                .build();

        GetMetricStatisticsResponse response = cloudWatchClient.getMetricStatistics(request);
        List<Datapoint> datapoints = response.datapoints();

        if (datapoints.isEmpty()) {
            return 0.0;
        }

        // Return the latest datapoint
        return datapoints.stream()
                .sorted(Comparator.comparing(Datapoint::timestamp).reversed())
                .findFirst()
                .map(dp -> {
                    if (statistic == Statistic.SUM)
                        return dp.sum();
                    if (statistic == Statistic.MAXIMUM)
                        return dp.maximum();
                    if (statistic == Statistic.AVERAGE)
                        return dp.average();
                    return 0.0;
                })
                .orElse(0.0);
    }
}
