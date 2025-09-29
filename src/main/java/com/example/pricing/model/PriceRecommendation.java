package com.example.pricing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_recommendations")
@Data
public class PriceRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productId;
    private String currency;
    private BigDecimal recommendedValue;
    private LocalDateTime recommendedAt;
    private String status;
}
