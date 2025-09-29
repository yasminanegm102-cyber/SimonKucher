package com.example.pricing.dto;

import lombok.Data;

@Data
public class PriceCsv {
    private String productId;
    private String currency;
    private String value;
    private String lastUpdated;
}
