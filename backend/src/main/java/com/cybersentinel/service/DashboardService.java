package com.cybersentinel.service;

import com.cybersentinel.dto.DashboardStatsDTO;
import com.cybersentinel.dto.ThreatDTO;
import com.cybersentinel.dto.TrafficPointDTO;
import com.cybersentinel.model.NetworkFlow;
import com.cybersentinel.model.Threat;
import com.cybersentinel.repository.NetworkFlowRepository;
import com.cybersentinel.repository.ThreatRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final List<String> CLASSES = List.of("Normal", "DDoS", "PortScan", "BruteForce");
    private static final DateTimeFormatter MINUTE_FORMAT =
            DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    private final NetworkFlowRepository networkFlowRepository;
    private final ThreatRepository threatRepository;
    private final ThreatService threatService;

    public DashboardService(NetworkFlowRepository networkFlowRepository,
                             ThreatRepository threatRepository,
                             ThreatService threatService) {
        this.networkFlowRepository = networkFlowRepository;
        this.threatRepository = threatRepository;
        this.threatService = threatService;
    }

    public DashboardStatsDTO getStats() {
        long totalFlows = networkFlowRepository.count();
        long threatsDetected = threatRepository.countByThreatTypeNot("Normal");
        long criticalThreats = threatRepository.countBySeverity("CRITICAL");

        List<Threat> recentThreats = threatRepository.findTop20ByOrderByDetectionTimeDesc();
        int currentRisk = recentThreats.stream()
                .filter(t -> !"Normal".equalsIgnoreCase(t.getThreatType()))
                .findFirst()
                .map(Threat::getRiskScore)
                .orElse(0);

        Map<String, Long> distribution = new LinkedHashMap<>();
        for (String cls : CLASSES) {
            distribution.put(cls, threatRepository.countByThreatType(cls));
        }

        List<ThreatDTO> liveFeed = recentThreats.stream()
                .filter(t -> !"Normal".equalsIgnoreCase(t.getThreatType()))
                .limit(10)
                .map(threatService::toDTO)
                .collect(Collectors.toList());

        return DashboardStatsDTO.builder()
                .totalFlows(totalFlows)
                .threatsDetected(threatsDetected)
                .criticalThreats(criticalThreats)
                .currentRisk(currentRisk)
                .threatDistribution(distribution)
                .trafficActivity(buildTrafficActivity())
                .liveThreatFeed(liveFeed)
                .build();
    }

    /** Buckets flows/threats from the last 20 minutes into 1-minute points for the chart. */
    private List<TrafficPointDTO> buildTrafficActivity() {
        Instant windowStart = Instant.now().minus(20, ChronoUnit.MINUTES);
        return bucketize(windowStart);
    }

    private List<TrafficPointDTO> bucketize(Instant windowStart) {
        List<NetworkFlow> flows = networkFlowRepository.findAll().stream()
                .filter(f -> f.getTimestamp() != null && f.getTimestamp().isAfter(windowStart))
                .collect(Collectors.toList());
        List<Threat> threats = threatRepository.findAllByOrderByDetectionTimeDesc().stream()
                .filter(t -> t.getDetectionTime() != null && t.getDetectionTime().isAfter(windowStart)
                        && !"Normal".equalsIgnoreCase(t.getThreatType()))
                .collect(Collectors.toList());

        Map<String, Long> flowBuckets = new LinkedHashMap<>();
        Map<String, Long> threatBuckets = new LinkedHashMap<>();

        for (NetworkFlow f : flows) {
            String label = MINUTE_FORMAT.format(f.getTimestamp());
            flowBuckets.merge(label, 1L, Long::sum);
        }
        for (Threat t : threats) {
            String label = MINUTE_FORMAT.format(t.getDetectionTime());
            threatBuckets.merge(label, 1L, Long::sum);
        }

        return flowBuckets.entrySet().stream()
                .map(e -> TrafficPointDTO.builder()
                        .label(e.getKey())
                        .flows(e.getValue())
                        .threats(threatBuckets.getOrDefault(e.getKey(), 0L))
                        .build())
                .collect(Collectors.toList());
    }
}
