package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Payload sent from Spring Boot to the FastAPI ML service's /predict endpoint. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictionRequest {
    private String sourceIP;
    private String destinationIP;
    private String protocol;
    private int sourcePort;
    private int destinationPort;
    private double flowDuration;
    private long packetCount;
    private long byteCount;
    private Double packetsPerSecond;
    private Double bytesPerSecond;
    private int dstConnCount60s;
    private int uniqueDstPorts60s;
    private int repeatConnSameDstPort60s;
}
