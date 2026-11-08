package com.cybersentinel.controller;

import com.cybersentinel.model.Alert;
import com.cybersentinel.service.AlertService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public List<Alert> getAlerts(@RequestParam(required = false, defaultValue = "false") boolean openOnly) {
        return openOnly ? alertService.getOpen() : alertService.getAll();
    }
}
