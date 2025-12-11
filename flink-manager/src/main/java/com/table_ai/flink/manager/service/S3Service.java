package com.table_ai.flink.manager.service;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.File;

@Service
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public String uploadJar(File jarFile, String workflowName) {
        String key = "jobs/" + workflowName + "/" + jarFile.getName();

        PutObjectRequest putOb = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .build();

        s3Client.putObject(putOb, RequestBody.fromFile(jarFile));

        return key;
    }

    public String getBucketArn() {
        return "arn:aws:s3:::" + bucketName;
    }
}
