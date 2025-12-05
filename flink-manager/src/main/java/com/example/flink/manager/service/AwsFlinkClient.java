package com.example.flink.manager.service;

import software.amazon.awssdk.services.kinesisanalyticsv2.KinesisAnalyticsV2Client;
import software.amazon.awssdk.services.kinesisanalyticsv2.model.*;

import org.springframework.stereotype.Service;

@Service
public class AwsFlinkClient {

        private final KinesisAnalyticsV2Client kinesisClient;

        public AwsFlinkClient(KinesisAnalyticsV2Client kinesisClient) {
                this.kinesisClient = kinesisClient;
        }

        public void deployApplication(String appName, String s3BucketArn, String s3ObjectKey) {
                // This is a simplified deployment logic.
                // 1. Check if app exists
                // 2. If not, create. If yes, update.
                // For brevity, showing Creation request structure.

                CreateApplicationRequest request = CreateApplicationRequest.builder()
                                .applicationName(appName)
                                .runtimeEnvironment(RuntimeEnvironment.FLINK_1_15) // Adjust version
                                .serviceExecutionRole("arn:aws:iam::123456789012:role/service-role/my-role") // Needs
                                                                                                             // config
                                .applicationConfiguration(ApplicationConfiguration.builder()
                                                .applicationCodeConfiguration(ApplicationCodeConfiguration.builder()
                                                                .codeContent(CodeContent.builder()
                                                                                .s3ContentLocation(S3ContentLocation
                                                                                                .builder()
                                                                                                .bucketARN(s3BucketArn)
                                                                                                .fileKey(s3ObjectKey)
                                                                                                .build())
                                                                                .build())
                                                                .codeContentType(CodeContentType.ZIPFILE)
                                                                .build())
                                                .build())
                                .build();

                try {
                        kinesisClient.createApplication(request);
                        System.out.println("Created AWS Flink Application: " + appName);

                        // Start the application
                        kinesisClient.startApplication(StartApplicationRequest.builder()
                                        .applicationName(appName)
                                        .runConfiguration(RunConfiguration.builder().build())
                                        .build());

                } catch (ResourceInUseException e) {
                        System.out.println("Application already exists, skipping creation.");
                        // Logic to update application code would go here (ListApplication,
                        // UpdateApplication)
                }
        }
}
