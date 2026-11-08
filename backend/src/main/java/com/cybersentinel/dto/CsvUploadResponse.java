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
public class CsvUploadResponse {
    private int totalFlowsAnalyzed;
    private Map<String, Long> classCounts;   // Normal/DDoS/PortScan/BruteForce
    private List<ThreatDTO> detectedThreats;
    private List<String> validationWarnings;
}
