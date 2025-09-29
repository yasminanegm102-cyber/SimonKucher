package com.example.pricing.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class AlgorithmConfigService {
    private volatile BigDecimal targetOccupancy = BigDecimal.valueOf(0.8);
    private volatile BigDecimal sensitivity = BigDecimal.valueOf(0.25);
    private volatile int windowDays = 30;

    public synchronized void update(BigDecimal targetOccupancy, BigDecimal sensitivity, Integer windowDays) {
        if (targetOccupancy != null) this.targetOccupancy = targetOccupancy;
        if (sensitivity != null) this.sensitivity = sensitivity;
        if (windowDays != null) this.windowDays = windowDays;
    }

    public BigDecimal getTargetOccupancy() { return targetOccupancy; }
    public BigDecimal getSensitivity() { return sensitivity; }
    public int getWindowDays() { return windowDays; }
}




