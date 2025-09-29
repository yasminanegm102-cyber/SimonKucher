package com.example.pricing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "prices")
@Data
public class Price {

    @EmbeddedId
    private PriceId id;

    private BigDecimal value;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
}
