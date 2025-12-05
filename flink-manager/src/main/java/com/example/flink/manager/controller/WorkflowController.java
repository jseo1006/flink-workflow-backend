package com.example.flink.manager.controller;

import com.example.flink.manager.dto.WorkflowDefinition;
import com.example.flink.manager.service.WorkflowService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    public WorkflowController(WorkflowService workflowService) {
        this.workflowService = workflowService;
    }

    @PostMapping("/deploy")
    public String deployWorkflow(@RequestBody WorkflowDefinition workflow) {
        try {
            return workflowService.deployWorkflow(workflow);
        } catch (Exception e) {
            e.printStackTrace();
            return "Deployment failed: " + e.getMessage();
        }
    }
}
