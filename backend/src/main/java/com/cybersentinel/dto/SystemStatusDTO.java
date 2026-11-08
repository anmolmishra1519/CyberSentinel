package com.cybersentinel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusDTO {
    private boolean mlServiceOnline;
    private boolean springBootOnline;   // if this endpoint responds, it's true
    private boolean mongoConnected;
    private long totalFlowsProcessed;
    private long uptimeSeconds;
}
