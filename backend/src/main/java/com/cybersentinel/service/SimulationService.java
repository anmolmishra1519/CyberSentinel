package com.cybersentinel.service;

import com.cybersentinel.dto.FlowProcessingResult;
import com.cybersentinel.dto.SimulationResultDTO;
import com.cybersentinel.model.NetworkFlow;
import com.cybersentinel.model.Threat;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class SimulationService {

    private final FlowSimulator flowSimulator;
    private final TrafficService trafficService;
    private final ThreatService threatService;

    private final AtomicReference<String> currentStatus = new AtomicReference<>("NORMAL");

    public SimulationService(FlowSimulator flowSimulator, TrafficService trafficService, ThreatService threatService) {
        this.flowSimulator = flowSimulator;
        this.trafficService = trafficService;
        this.threatService = threatService;
    }

    public SimulationResultDTO runSimulation(String attackType) {
        List<NetworkFlow> burst = flowSimulator.generateBurst(attackType);
        List<FlowProcessingResult> results = trafficService.processFlows(burst, "SIMULATION");

        FlowProcessingResult top = results.stream()
                .max(Comparator.comparingInt(FlowProcessingResult::getRiskScore))
                .orElse(null);

        currentStatus.set(top != null && !"Normal".equalsIgnoreCase(top.getPrediction()) ? top.getPrediction() : "NORMAL");

        var headline = top != null
                ? threatService.getById(top.getThreatId()).map(threatService::toDTO).orElse(null)
                : null;

        return SimulationResultDTO.builder()
                .attackType(attackType)
                .flowsGenerated(burst.size())
                .headlineThreat(headline)
                .allResults(results)
                .build();
    }

    public String getCurrentStatus() {
        return currentStatus.get();
    }

    public void resetToNormal() {
        currentStatus.set("NORMAL");
    }
}
