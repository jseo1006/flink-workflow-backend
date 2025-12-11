package com.table_ai.flink.manager.controller;

import com.table_ai.flink.manager.dto.WorkflowDefinition;
import com.table_ai.flink.manager.service.WorkflowService;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/deploy")
    public String deployWorkflow(@RequestBody WorkflowDefinition workflow, Principal principal) {
        try {
            // Fallback to "default-user" if Principal is null (e.g. running behind Gateway
            // without token relay)
            String userId = (principal != null) ? principal.getName() : "default-user";
            return workflowService.deployWorkflow(workflow, userId);
        } catch (Exception e) {
            e.printStackTrace();
            return "Deployment failed: " + e.getMessage();
        }
    }
}
