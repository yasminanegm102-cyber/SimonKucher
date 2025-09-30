package com.example.pricing.controller;

import com.example.pricing.service.AlgorithmConfigService;
import com.example.pricing.model.User;
import com.example.pricing.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class AlgorithmConfigController {
    private final AlgorithmConfigService service;
    private final UserRepository userRepository;

    public AlgorithmConfigController(AlgorithmConfigService service, UserRepository userRepository) {
        this.service = service;
        this.userRepository = userRepository;
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
    public Map<String, Object> updateConfig(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        // Authorization check: only ADMIN users can update algorithm config
        if (userId != null && !userId.isBlank()) {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid user");
            }
            if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, 
                    "Only ADMIN users can update algorithm configuration. Your role: " + user.getRole());
            }
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, 
                "X-User-Id header is required for authorization");
        }
        
        BigDecimal target = body.get("targetOccupancy") == null ? null : new BigDecimal(body.get("targetOccupancy").toString());
        BigDecimal sens = body.get("sensitivity") == null ? null : new BigDecimal(body.get("sensitivity").toString());
        Integer window = body.get("windowDays") == null ? null : Integer.valueOf(body.get("windowDays").toString());
        service.update(target, sens, window);
        return getConfig();
    }
}




