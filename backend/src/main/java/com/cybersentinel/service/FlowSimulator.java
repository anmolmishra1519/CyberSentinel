package com.cybersentinel.service;

import com.cybersentinel.model.NetworkFlow;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates REPRESENTATIVE, realistic-looking raw network flows for the
 * Attack Simulator - nothing more. It does NOT decide what the traffic
 * "is"; no label is attached to these flows. They are sent through the
 * exact same TrafficService.processFlow() pipeline as a CSV upload, and the
 * ML model is what ultimately classifies them. This is what stops the
 * simulator from being a hardcoded "DDoS detected" button.
 */
@Component
public class FlowSimulator {

    private final Random random = new Random();
    private static final int[] AUTH_PORTS = {22, 21, 3389, 23, 3306};
    private static final int[] COMMON_PORTS = {80, 443, 53, 22, 25, 110, 143, 3306, 8080, 8443};

    public List<NetworkFlow> generateBurst(String attackType) {
        return switch (attackType.toUpperCase()) {
            case "NORMAL" -> normalBurst();
            case "DDOS" -> ddosBurst();
            case "PORT_SCAN" -> portScanBurst();
            case "BRUTE_FORCE" -> bruteForceBurst();
            default -> throw new IllegalArgumentException("Unknown attack type: " + attackType);
        };
    }

    private List<NetworkFlow> normalBurst() {
        List<NetworkFlow> flows = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            flows.add(NetworkFlow.builder()
                    .sourceIP(randomInternalIp())
                    .destinationIP(randomServerIp())
                    .protocol(random.nextDouble() < 0.8 ? "TCP" : "UDP")
                    .sourcePort(randomEphemeralPort())
                    .destinationPort(COMMON_PORTS[random.nextInt(COMMON_PORTS.length)])
                    .flowDuration(round(0.5 + random.nextDouble() * 29.5))
                    .packetCount(8 + random.nextInt(210))
                    .byteCount(3000 + random.nextInt(90000))
                    .timestamp(Instant.now())
                    .build());
        }
        return flows;
    }

    private List<NetworkFlow> ddosBurst() {
        List<NetworkFlow> flows = new ArrayList<>();
        String victim = randomServerIp();
        int targetPort = new int[]{80, 443, 53}[random.nextInt(3)];
        int botCount = 30 + random.nextInt(20);
        for (int i = 0; i < botCount; i++) {
            long packetCount = 300 + random.nextInt(4200);
            double avgPacketSize = 60 + random.nextInt(60);
            flows.add(NetworkFlow.builder()
                    .sourceIP(randomExternalIp())
                    .destinationIP(victim)
                    .protocol(random.nextDouble() < 0.7 ? "TCP" : "UDP")
                    .sourcePort(randomEphemeralPort())
                    .destinationPort(targetPort)
                    .flowDuration(round(0.01 + random.nextDouble() * 1.2))
                    .packetCount(packetCount)
                    .byteCount((long) (packetCount * avgPacketSize))
                    .timestamp(Instant.now())
                    .build());
        }
        return flows;
    }

    private List<NetworkFlow> portScanBurst() {
        List<NetworkFlow> flows = new ArrayList<>();
        String scanner = randomExternalIp();
        String target = randomServerIp();
        int nPorts = 40 + random.nextInt(120);
        for (int i = 0; i < nPorts; i++) {
            int packetCount = 1 + random.nextInt(2);
            flows.add(NetworkFlow.builder()
                    .sourceIP(scanner)
                    .destinationIP(target)
                    .protocol("TCP")
                    .sourcePort(randomEphemeralPort())
                    .destinationPort(1 + random.nextInt(9999))
                    .flowDuration(round(0.001 + random.nextDouble() * 0.08))
                    .packetCount(packetCount)
                    .byteCount((long) (packetCount * (40 + random.nextInt(20))))
                    .timestamp(Instant.now())
                    .build());
        }
        return flows;
    }

    private List<NetworkFlow> bruteForceBurst() {
        List<NetworkFlow> flows = new ArrayList<>();
        String attacker = randomExternalIp();
        String target = randomServerIp();
        int targetPort = AUTH_PORTS[random.nextInt(AUTH_PORTS.length)];
        int attempts = 25 + random.nextInt(50);
        for (int i = 0; i < attempts; i++) {
            int packetCount = 6 + random.nextInt(34);
            flows.add(NetworkFlow.builder()
                    .sourceIP(attacker)
                    .destinationIP(target)
                    .protocol("TCP")
                    .sourcePort(randomEphemeralPort())
                    .destinationPort(targetPort)
                    .flowDuration(round(0.3 + random.nextDouble() * 3.2))
                    .packetCount(packetCount)
                    .byteCount((long) (packetCount * (140 + random.nextInt(80))))
                    .timestamp(Instant.now())
                    .build());
        }
        return flows;
    }

    private String randomInternalIp() {
        return "192.168.1." + (1 + random.nextInt(254));
    }

    private String randomServerIp() {
        return "10.0.0." + (1 + random.nextInt(254));
    }

    private String randomExternalIp() {
        return "203.0." + (1 + random.nextInt(59)) + "." + (1 + random.nextInt(254));
    }

    private int randomEphemeralPort() {
        return 1024 + random.nextInt(64511);
    }

    private double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
