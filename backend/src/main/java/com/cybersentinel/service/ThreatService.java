package com.cybersentinel.service;

import com.cybersentinel.dto.PredictionResponse;
import com.cybersentinel.dto.ThreatDTO;
import com.cybersentinel.model.NetworkFlow;
import com.cybersentinel.model.Threat;
import com.cybersentinel.repository.ThreatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
public class ThreatService {

    private final ThreatRepository threatRepository;

    public ThreatService(ThreatRepository threatRepository) {
        this.threatRepository = threatRepository;
    }

    public Threat createFromPrediction(NetworkFlow flow, PredictionResponse prediction) {
        Threat threat = Threat.builder()
                .networkFlowId(flow.getId())
                .threatType(prediction.getPrediction())
                .sourceIP(flow.getSourceIP())
                .destinationIP(flow.getDestinationIP())
                .protocol(flow.getProtocol())
                .sourcePort(flow.getSourcePort())
                .destinationPort(flow.getDestinationPort())
                .flowDuration(flow.getFlowDuration())
                .packetCount(flow.getPacketCount())
                .byteCount(flow.getByteCount())
                .confidence(prediction.getConfidence())
                .riskScore(prediction.getRiskScore())
                .severity(prediction.getSeverity())
                .reasons(prediction.getReasons())
                .recommendedAction(prediction.getRecommendedAction())
                .detectionTime(Instant.now())
                .status("OPEN")
                .build();
        return threatRepository.save(threat);
    }

    public List<Threat> getAll() {
        return threatRepository.findAllByOrderByDetectionTimeDesc();
    }

    public List<Threat> getByType(String type) {
        return threatRepository.findByThreatTypeOrderByDetectionTimeDesc(type);
    }

    public List<Threat> getBySeverity(String severity) {
        return threatRepository.findBySeverityOrderByDetectionTimeDesc(severity);
    }

    public Optional<Threat> getById(String id) {
        return threatRepository.findById(id);
    }

    public List<Threat> getRecent(int limit) {
        return threatRepository.findTop20ByOrderByDetectionTimeDesc();
    }

    public long countThreats() {
        return threatRepository.countByThreatTypeNot("Normal");
    }

    public long countCritical() {
        return threatRepository.countBySeverity("CRITICAL");
    }

    public ThreatDTO toDTO(Threat t) {
        return ThreatDTO.builder()
                .id(t.getId())
                .threatType(t.getThreatType())
                .sourceIP(t.getSourceIP())
                .destinationIP(t.getDestinationIP())
                .protocol(t.getProtocol())
                .sourcePort(t.getSourcePort())
                .destinationPort(t.getDestinationPort())
                .flowDuration(t.getFlowDuration())
                .packetCount(t.getPacketCount())
                .byteCount(t.getByteCount())
                .confidence(t.getConfidence())
                .riskScore(t.getRiskScore())
                .severity(t.getSeverity())
                .reasons(t.getReasons())
                .recommendedAction(t.getRecommendedAction())
                .detectionTime(t.getDetectionTime())
                .status(t.getStatus())
                .build();
    }
}
