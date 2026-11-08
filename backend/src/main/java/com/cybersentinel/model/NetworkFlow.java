package com.cybersentinel.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * A single unidirectional network-flow record.
 * Collection: network_flows
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "network_flows")
public class NetworkFlow {

    @Id
    private String id;

    private String sourceIP;
    private String destinationIP;
    private String protocol;
    private int sourcePort;
    private int destinationPort;

    private double flowDuration;
    private long packetCount;
    private long byteCount;
    private double packetsPerSecond;
    private double bytesPerSecond;

    // Contextual/aggregate features computed by the backend from recent
    // flows in MongoDB before this flow is sent to the ML service.
    private int dstConnCount60s;
    private int uniqueDstPorts60s;
    private int repeatConnSameDstPort60s;

    private Instant timestamp;

    /** "UPLOAD" or "SIMULATION" - lets the dashboard distinguish flow sources */
    private String source;
}
