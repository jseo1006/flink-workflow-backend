package com.table_ai.flink.manager.service;

import software.amazon.awssdk.services.kinesisanalyticsv2.KinesisAnalyticsV2Client;
import software.amazon.awssdk.services.kinesisanalyticsv2.model.*;

import org.springframework.stereotype.Service;

@Service
public class AwsFlinkClient {

        @org.springframework.beans.factory.annotation.Value("${aws.role-arn}")
        private String serviceExecutionRoleArn;

        private final KinesisAnalyticsV2Client kinesisClient;

        public AwsFlinkClient(KinesisAnalyticsV2Client kinesisClient) {
                this.kinesisClient = kinesisClient;
        }

        public void deployApplication(String appName, String s3BucketArn, String s3ObjectKey) {
                // 1. Delete existing application if it exists (Force Update)
                deleteApplicationIfExists(appName);

                // 2. Create Application
                CreateApplicationRequest request = CreateApplicationRequest.builder()
                                .applicationName(appName)
                                .runtimeEnvironment(RuntimeEnvironment.FLINK_1_15)
                                .serviceExecutionRole(serviceExecutionRoleArn)
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

                kinesisClient.createApplication(request);
                System.out.println("Created AWS Flink Application: " + appName);

                // 3. Start the application
                kinesisClient.startApplication(StartApplicationRequest.builder()
                                .applicationName(appName)
                                .runConfiguration(RunConfiguration.builder().build())
                                .build());
        }

        private void deleteApplicationIfExists(String appName) {
                try {
                        DescribeApplicationResponse describe = kinesisClient
                                        .describeApplication(DescribeApplicationRequest.builder()
                                                        .applicationName(appName)
                                                        .build());

                        long createTimestamp = describe.applicationDetail().createTimestamp().toEpochMilli(); // SDK v2
                                                                                                              // uses
                                                                                                              // Instant?
                        // describe.applicationDetail().createTimestamp() returns Instant.
                        // DeleteApplicationRequest builder takes Instant.

                        System.out.println("Deleting existing application: " + appName);
                        kinesisClient.deleteApplication(DeleteApplicationRequest.builder()
                                        .applicationName(appName)
                                        .createTimestamp(describe.applicationDetail().createTimestamp())
                                        .build());

                        // Wait for deletion
                        while (true) {
                                try {
                                        Thread.sleep(2000);
                                        kinesisClient.describeApplication(DescribeApplicationRequest.builder()
                                                        .applicationName(appName)
                                                        .build());
                                        System.out.println("Waiting for deletion...");
                                } catch (ResourceNotFoundException e) {
                                        break; // Deleted
                                }
                        }
                        System.out.println("Application deleted.");

                } catch (ResourceNotFoundException e) {
                        // Not found, safe to create
                } catch (Exception e) {
                        throw new RuntimeException("Failed to delete application", e);
                }
        }
}
