package com.cybersentinel.controller;

import com.cybersentinel.dto.ThreatDTO;
import com.cybersentinel.model.Threat;
import com.cybersentinel.service.ThreatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/threats")
public class ThreatController {

    private final ThreatService threatService;

    public ThreatController(ThreatService threatService) {
        this.threatService = threatService;
    }

    @GetMapping
    public List<ThreatDTO> getThreats(@RequestParam(required = false) String type,
                                       @RequestParam(required = false) String severity) {
        List<Threat> threats;
        if (type != null && !type.isBlank() && !type.equalsIgnoreCase("All")) {
            threats = threatService.getByType(type);
        } else if (severity != null && !severity.isBlank()) {
            threats = threatService.getBySeverity(severity.toUpperCase());
        } else {
            threats = threatService.getAll();
        }
        return threats.stream().map(threatService::toDTO).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ThreatDTO> getThreat(@PathVariable String id) {
        return threatService.getById(id)
                .map(threatService::toDTO)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
