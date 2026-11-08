package com.cybersentinel.service;

import com.cybersentinel.model.Alert;
import com.cybersentinel.model.Threat;
import com.cybersentinel.repository.AlertRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    public AlertService(AlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    public Alert createFromThreat(Threat threat) {
        Alert alert = Alert.builder()
                .threatId(threat.getId())
                .threatType(threat.getThreatType())
                .severity(threat.getSeverity())
                .status("OPEN")
                .createdAt(Instant.now())
                .build();
        return alertRepository.save(alert);
    }

    public List<Alert> getAll() {
        return alertRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Alert> getOpen() {
        return alertRepository.findByStatusOrderByCreatedAtDesc("OPEN");
    }
}
