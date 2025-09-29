package com.example.pricing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_confirmations")
@Data
public class PriceConfirmation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String productId;
    private String action;
    private BigDecimal confirmedValue;
    private String currency;
    private String userId;
    private LocalDateTime confirmedAt;
    private Boolean synced = false;
}
