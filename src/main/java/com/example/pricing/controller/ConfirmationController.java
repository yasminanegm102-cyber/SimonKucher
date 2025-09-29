package com.example.pricing.controller;

import com.example.pricing.service.ConfirmationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/confirmations")
public class ConfirmationController {
    private final ConfirmationService service;

    public ConfirmationController(ConfirmationService service) {
        this.service = service;
    }

    @PostMapping("/batch")
    public List<Map<String, Object>> confirmBatch(@RequestBody List<Map<String,Object>> requests) {
        List<Map<String,Object>> results = new ArrayList<>();
        for (Map<String,Object> req : requests) {
            try {
                String productId = (String) req.get("productId");
                String action = (String) req.get("action");
                BigDecimal price = req.get("price") == null ? null : new BigDecimal(req.get("price").toString());
                String currency = (String) req.get("currency");
                String userId = (String) req.get("userId");

                service.confirm(productId, action, price, currency, userId);
                results.add(Map.of("productId", productId, "status", "success"));
            } catch (Exception ex) {
                results.add(Map.of("productId", req.get("productId"), "status", "failed", "error", ex.getMessage()));
            }
        }
        return results;
    }
}
