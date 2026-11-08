package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationResultDTO {
    private String attackType;
    private int flowsGenerated;
    private ThreatDTO headlineThreat;   // the highest-risk result from this burst, for the "killer moment"
    private List<FlowProcessingResult> allResults;
}
