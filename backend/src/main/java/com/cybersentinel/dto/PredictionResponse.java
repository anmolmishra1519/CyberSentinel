package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/** Response returned by the FastAPI ML service's /predict endpoint. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PredictionResponse {
    private String prediction;
    private double confidence;
    private int riskScore;
    private String severity;
    private List<String> reasons;
    private String recommendedAction;
    private Map<String, Double> classProbabilities;
}
