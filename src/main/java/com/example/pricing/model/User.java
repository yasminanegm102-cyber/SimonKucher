package com.example.pricing.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {
    @Id
    private String id;
    private String name;
    private String role; // e.g., PRICING_MANAGER, REGIONAL_MANAGER, REPORTING
    private String region; // e.g., EMEA, AMER, APAC
}


