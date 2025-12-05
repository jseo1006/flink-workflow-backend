package com.example.flink.manager.controller;

import com.example.flink.manager.service.CepRuleService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cep")
public class CepRuleController {

    private final CepRuleService cepRuleService;

    public CepRuleController(CepRuleService cepRuleService) {
        this.cepRuleService = cepRuleService;
    }

    @PostMapping("/rules/{jobId}")
    public void updateRule(@PathVariable String jobId, @RequestBody String ruleJson) {
        cepRuleService.updateRule(jobId, ruleJson);
    }
}
