package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsDTO {
    private long totalFlows;
    private long threatsDetected;
    private long criticalThreats;
    private int currentRisk;
    private Map<String, Long> threatDistribution;   // Normal/DDoS/PortScan/BruteForce counts
    private List<TrafficPointDTO> trafficActivity;   // time-series for the chart
    private List<ThreatDTO> liveThreatFeed;           // most recent threats
}
