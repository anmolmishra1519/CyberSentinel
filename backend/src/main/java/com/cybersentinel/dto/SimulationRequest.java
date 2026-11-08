package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationRequest {
    /** NORMAL | DDOS | PORT_SCAN | BRUTE_FORCE */
    private String attackType;
}
