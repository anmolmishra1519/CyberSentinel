package com.cybersentinel.controller;

import com.cybersentinel.dto.SimulationRequest;
import com.cybersentinel.dto.SimulationResultDTO;
import com.cybersentinel.service.SimulationService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/simulation")
public class SimulationController {

    private final SimulationService simulationService;

    public SimulationController(SimulationService simulationService) {
        this.simulationService = simulationService;
    }

    @PostMapping("/start")
    public SimulationResultDTO start(@RequestBody SimulationRequest request) {
        if (request.getAttackType() == null || request.getAttackType().isBlank()) {
            throw new IllegalArgumentException("attackType is required (NORMAL, DDOS, PORT_SCAN, BRUTE_FORCE).");
        }
        return simulationService.runSimulation(request.getAttackType());
    }

    @GetMapping("/status")
    public Map<String, String> status() {
        return Map.of("status", simulationService.getCurrentStatus());
    }
}
