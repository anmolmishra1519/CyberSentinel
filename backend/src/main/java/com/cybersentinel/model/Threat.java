package com.cybersentinel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.List;

/**
 * A threat produced by the ML pipeline for a given network flow.
 * Collection: threats
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "threats")
public class Threat {

    @Id
    private String id;

    private String networkFlowId;

    private String threatType;      // Normal | DDoS | PortScan | BruteForce
    private String sourceIP;
    private String destinationIP;
    private String protocol;
    private int sourcePort;
    private int destinationPort;

    private double flowDuration;
    private long packetCount;
    private long byteCount;

    private double confidence;      // 0.0 - 1.0, from the ML model
    private int riskScore;          // 0 - 100, computed
    private String severity;        // SAFE | LOW | MEDIUM | HIGH | CRITICAL

    private List<String> reasons;
    private String recommendedAction;

    private Instant detectionTime;
    private String status;          // OPEN | INVESTIGATING | RESOLVED
}
