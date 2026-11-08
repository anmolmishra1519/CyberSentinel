package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

/** Threat shape returned to the React frontend. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ThreatDTO {
    private String id;
    private String threatType;
    private String sourceIP;
    private String destinationIP;
    private String protocol;
    private int sourcePort;
    private int destinationPort;
    private double flowDuration;
    private long packetCount;
    private long byteCount;
    private double confidence;
    private int riskScore;
    private String severity;
    private List<String> reasons;
    private String recommendedAction;
    private Instant detectionTime;
    private String status;
}
