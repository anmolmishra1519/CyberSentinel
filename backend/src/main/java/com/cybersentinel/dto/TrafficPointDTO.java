package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrafficPointDTO {
    private String label;   // e.g. "09:41"
    private long flows;
    private long threats;
}
