package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FlowProcessingResult {
    private String flowId;
    private String threatId;
    private String prediction;
    private double confidence;
    private int riskScore;
    private String severity;
}
