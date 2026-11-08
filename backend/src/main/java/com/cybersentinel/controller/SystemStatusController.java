package com.cybersentinel.controller;

import com.cybersentinel.dto.SystemStatusDTO;
import com.cybersentinel.repository.NetworkFlowRepository;
import com.cybersentinel.service.MLService;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemStatusController {

    private final MLService mlService;
    private final MongoTemplate mongoTemplate;
    private final NetworkFlowRepository networkFlowRepository;
    private final long startTime = System.currentTimeMillis();

    public SystemStatusController(MLService mlService, MongoTemplate mongoTemplate,
                                   NetworkFlowRepository networkFlowRepository) {
        this.mlService = mlService;
        this.mongoTemplate = mongoTemplate;
        this.networkFlowRepository = networkFlowRepository;
    }

    @GetMapping("/status")
    public SystemStatusDTO getStatus() {
        boolean mongoConnected;
        long totalFlows = 0;
        try {
            totalFlows = networkFlowRepository.count();
            mongoConnected = true;
        } catch (Exception e) {
            mongoConnected = false;
        }

        boolean mlOnline = mlService.isMlServiceHealthy();

        return SystemStatusDTO.builder()
                .mlServiceOnline(mlOnline)
                .springBootOnline(true)
                .mongoConnected(mongoConnected)
                .totalFlowsProcessed(totalFlows)
                .uptimeSeconds((System.currentTimeMillis() - startTime) / 1000)
                .build();
    }
}
