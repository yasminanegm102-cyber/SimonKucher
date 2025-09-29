package com.example.pricing.controller;

import com.example.pricing.service.AlgorithmConfigService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class AlgorithmConfigController {
    private final AlgorithmConfigService service;

    public AlgorithmConfigController(AlgorithmConfigService service) {
        this.service = service;
    }

    @GetMapping("/pricing")
    public Map<String, Object> getConfig() {
        return Map.of(
                "targetOccupancy", service.getTargetOccupancy(),
                "sensitivity", service.getSensitivity(),
                "windowDays", service.getWindowDays()
        );
    }

    @PutMapping("/pricing")
    public Map<String, Object> updateConfig(@RequestBody Map<String, Object> body) {
        BigDecimal target = body.get("targetOccupancy") == null ? null : new BigDecimal(body.get("targetOccupancy").toString());
        BigDecimal sens = body.get("sensitivity") == null ? null : new BigDecimal(body.get("sensitivity").toString());
        Integer window = body.get("windowDays") == null ? null : Integer.valueOf(body.get("windowDays").toString());
        service.update(target, sens, window);
        return getConfig();
    }
}


