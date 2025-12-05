package com.example.flink.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.kinesisanalyticsv2.KinesisAnalyticsV2Client;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    // Default to US_EAST_1 if not specified, or read from env
    private final Region region = Region.US_EAST_1;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(region)
                .build();
    }

    @Bean
    public KinesisAnalyticsV2Client kinesisAnalyticsV2Client() {
        return KinesisAnalyticsV2Client.builder()
                .region(region)
                .build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(region)
                .build();
    }
}
