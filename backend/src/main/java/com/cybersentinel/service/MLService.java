package com.cybersentinel.service;

import com.cybersentinel.client.MLClient;
import com.cybersentinel.dto.PredictionRequest;
import com.cybersentinel.dto.PredictionResponse;
import com.cybersentinel.model.NetworkFlow;
import org.springframework.stereotype.Service;

/**
 * Thin bridge between the domain model (NetworkFlow) and the ML client's
 * request/response DTOs. This is the ONLY place a NetworkFlow is turned
 * into a prediction request - kept separate from TrafficService so the ML
 * integration can be unit tested / swapped independently.
 */
@Service
public class MLService {

    private final MLClient mlClient;

    public MLService(MLClient mlClient) {
        this.mlClient = mlClient;
    }

    public PredictionResponse getPrediction(NetworkFlow flow) {
        PredictionRequest request = PredictionRequest.builder()
                .sourceIP(flow.getSourceIP())
                .destinationIP(flow.getDestinationIP())
                .protocol(flow.getProtocol())
                .sourcePort(flow.getSourcePort())
                .destinationPort(flow.getDestinationPort())
                .flowDuration(flow.getFlowDuration())
                .packetCount(flow.getPacketCount())
                .byteCount(flow.getByteCount())
                .packetsPerSecond(flow.getPacketsPerSecond())
                .bytesPerSecond(flow.getBytesPerSecond())
                .dstConnCount60s(flow.getDstConnCount60s())
                .uniqueDstPorts60s(flow.getUniqueDstPorts60s())
                .repeatConnSameDstPort60s(flow.getRepeatConnSameDstPort60s())
                .build();

        return mlClient.predict(request);
    }

    public boolean isMlServiceHealthy() {
        return mlClient.isHealthy();
    }
}
