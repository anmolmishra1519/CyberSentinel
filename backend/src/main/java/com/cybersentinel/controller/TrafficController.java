package com.cybersentinel.controller;

import com.cybersentinel.dto.CsvUploadResponse;
import com.cybersentinel.dto.FlowProcessingResult;
import com.cybersentinel.dto.ThreatDTO;
import com.cybersentinel.model.NetworkFlow;
import com.cybersentinel.service.CsvFlowParser;
import com.cybersentinel.service.ThreatService;
import com.cybersentinel.service.TrafficService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/traffic")
public class TrafficController {

    private final CsvFlowParser csvFlowParser;
    private final TrafficService trafficService;
    private final ThreatService threatService;

    public TrafficController(CsvFlowParser csvFlowParser, TrafficService trafficService, ThreatService threatService) {
        this.csvFlowParser = csvFlowParser;
        this.trafficService = trafficService;
        this.threatService = threatService;
    }

    /** Validates a CSV without running it through the ML pipeline - used by the "Selected File" preview step. */
    @PostMapping("/validate")
    public Map<String, Object> validate(@RequestParam("file") MultipartFile file) {
        CsvFlowParser.ParseResult result = csvFlowParser.parse(file);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("valid", true);
        response.put("flowCount", result.flows.size());
        response.put("warnings", result.warnings);
        return response;
    }

    /** Parses + runs every flow in the CSV through save -> ML -> threat -> alert. */
    @PostMapping("/analyze")
    public CsvUploadResponse analyze(@RequestParam("file") MultipartFile file) {
        CsvFlowParser.ParseResult parsed = csvFlowParser.parse(file);
        List<NetworkFlow> flows = parsed.flows;

        List<FlowProcessingResult> results = trafficService.processFlows(flows, "UPLOAD");

        Map<String, Long> classCounts = new LinkedHashMap<>();
        for (String cls : List.of("Normal", "DDoS", "PortScan", "BruteForce")) {
            classCounts.put(cls, 0L);
        }
        for (FlowProcessingResult r : results) {
            classCounts.merge(r.getPrediction(), 1L, Long::sum);
        }

        List<ThreatDTO> detectedThreats = results.stream()
                .filter(r -> !"Normal".equalsIgnoreCase(r.getPrediction()))
                .map(r -> threatService.getById(r.getThreatId()).map(threatService::toDTO).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return CsvUploadResponse.builder()
                .totalFlowsAnalyzed(results.size())
                .classCounts(classCounts)
                .detectedThreats(detectedThreats)
                .validationWarnings(parsed.warnings)
                .build();
    }
}
