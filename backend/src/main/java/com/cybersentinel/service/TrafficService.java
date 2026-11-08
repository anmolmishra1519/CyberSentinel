package com.cybersentinel.service;

import com.cybersentinel.dto.FlowProcessingResult;
import com.cybersentinel.dto.PredictionResponse;
import com.cybersentinel.model.NetworkFlow;
import com.cybersentinel.model.Threat;
import com.cybersentinel.repository.NetworkFlowRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Orchestrates the full pipeline for ONE network flow, whether it came from
 * a CSV upload or the Attack Simulator:
 *
 *   save raw flow -> compute contextual features -> call ML service
 *   -> save Threat -> save Alert (if not Normal)
 *
 * Both TrafficController (CSV upload) and SimulationController (simulator)
 * call this same method - there is no shortcut that fabricates a threat.
 */
@Service
public class TrafficService {

    private final NetworkFlowRepository networkFlowRepository;
    private final FlowContextService flowContextService;
    private final MLService mlService;
    private final ThreatService threatService;
    private final AlertService alertService;

    public TrafficService(NetworkFlowRepository networkFlowRepository,
                           FlowContextService flowContextService,
                           MLService mlService,
                           ThreatService threatService,
                           AlertService alertService) {
        this.networkFlowRepository = networkFlowRepository;
        this.flowContextService = flowContextService;
        this.mlService = mlService;
        this.threatService = threatService;
        this.alertService = alertService;
    }

    public FlowProcessingResult processFlow(NetworkFlow flow, String source) {
        if (flow.getTimestamp() == null) {
            flow.setTimestamp(Instant.now());
        }
        flow.setSource(source);

        if (flow.getPacketsPerSecond() == 0 && flow.getFlowDuration() > 0) {
            flow.setPacketsPerSecond(flow.getPacketCount() / Math.max(flow.getFlowDuration(), 0.001));
        }
        if (flow.getBytesPerSecond() == 0 && flow.getFlowDuration() > 0) {
            flow.setBytesPerSecond(flow.getByteCount() / Math.max(flow.getFlowDuration(), 0.001));
        }

        // context must be computed BEFORE this flow is saved, otherwise it
        // would count itself
        flowContextService.enrichWithContext(flow);

        NetworkFlow saved = networkFlowRepository.save(flow);

        PredictionResponse prediction = mlService.getPrediction(saved);

        Threat threat = threatService.createFromPrediction(saved, prediction);

        if (!"Normal".equalsIgnoreCase(prediction.getPrediction())) {
            alertService.createFromThreat(threat);
        }

        return FlowProcessingResult.builder()
                .flowId(saved.getId())
                .threatId(threat.getId())
                .prediction(prediction.getPrediction())
                .confidence(prediction.getConfidence())
                .riskScore(prediction.getRiskScore())
                .severity(prediction.getSeverity())
                .build();
    }

    public List<FlowProcessingResult> processFlows(List<NetworkFlow> flows, String source) {
        List<FlowProcessingResult> results = new ArrayList<>();
        for (NetworkFlow flow : flows) {
            results.add(processFlow(flow, source));
        }
        return results;
    }

    public long totalFlows() {
        return networkFlowRepository.count();
    }
}
