package com.table_ai.flink.manager.service;

import com.table_ai.flink.manager.dto.WorkflowDefinition;
import org.springframework.stereotype.Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.table_ai.flink.manager.entity.Workflow;
import com.table_ai.flink.manager.entity.WorkflowStatus;
import com.table_ai.flink.manager.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WorkflowService {

    private final CodeGeneratorService codeGeneratorService;
    private final BuildService buildService;
    private final S3Service s3Service;
    private final AwsFlinkClient awsFlinkClient;
    private final WorkflowRepository workflowRepository;
    private final ObjectMapper objectMapper; // For JSON serialization

    public WorkflowService(CodeGeneratorService codeGeneratorService, BuildService buildService,
            S3Service s3Service, AwsFlinkClient awsFlinkClient, WorkflowRepository workflowRepository,
            ObjectMapper objectMapper) {
        this.codeGeneratorService = codeGeneratorService;
        this.buildService = buildService;
        this.s3Service = s3Service;
        this.awsFlinkClient = awsFlinkClient;
        this.workflowRepository = workflowRepository;
        this.objectMapper = objectMapper;
    }

    public String deployWorkflow(WorkflowDefinition workflow, String userId) throws Exception {
        // 0. Save Workflow Metadata to DB (DRAFT)
        Workflow entity = new Workflow();
        entity.setName(workflow.getName());
        entity.setDescription(workflow.getDescription());
        entity.setDefinitionJson(objectMapper.writeValueAsString(workflow));
        entity.setStatus(WorkflowStatus.DRAFT);
        entity.setUserId(userId);
        workflowRepository.save(entity);

        // 1. Generate Code
        String javaCode = codeGeneratorService.generateJobCode(workflow);

        // 2. Prepare Build Directory (Temp)
        Path buildDir = Files.createTempDirectory("flink-job-" + workflow.getName());

        // 3. Write Code to File
        File sourceFile = writeCodeToDisk(buildDir, workflow.getName(), javaCode);

        // 4. Build JAR
        File jarFile = buildService.buildJar(buildDir, sourceFile);

        // 5. Upload to S3
        String s3Path = s3Service.uploadJar(jarFile, workflow.getName());

        // 6. Deploy to AWS Managed Flink
        awsFlinkClient.deployApplication(workflow.getName(), s3Service.getBucketArn(), s3Path);

        // 7. Update DB Status (DEPLOYED)
        entity.setStatus(WorkflowStatus.DEPLOYED);
        workflowRepository.save(entity);

        return "Job " + workflow.getName() + " deployed to AWS. S3 Path: " + s3Path;
    }

    private File writeCodeToDisk(Path buildDir, String jobName, String code) throws IOException {
        // Create full package path: src/main/java/com.table_ai/flink/generated
        Path packageDir = buildDir.resolve("src/main/java/com.table_ai/flink/generated");
        Files.createDirectories(packageDir);

        Path filePath = packageDir.resolve(jobName + "Job.java");
        Files.write(filePath, code.getBytes(), StandardOpenOption.CREATE);
        return filePath.toFile();
    }
}
