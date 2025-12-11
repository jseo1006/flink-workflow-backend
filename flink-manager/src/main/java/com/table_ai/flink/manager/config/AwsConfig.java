package com.table_ai.flink.manager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cloudwatch.CloudWatchClient;
import software.amazon.awssdk.services.kinesisanalyticsv2.KinesisAnalyticsV2Client;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @org.springframework.beans.factory.annotation.Value("${aws.region}")
    private String regionString;

    private Region getRegion() {
        return Region.of(regionString);
    }

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(getRegion())
                .build();
    }

    @Bean
    public KinesisAnalyticsV2Client kinesisAnalyticsV2Client() {
        return KinesisAnalyticsV2Client.builder()
                .region(getRegion())
                .build();
    }

    @Bean
    public CloudWatchClient cloudWatchClient() {
        return CloudWatchClient.builder()
                .region(getRegion())
                .build();
    }
}
