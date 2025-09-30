package com.example.pricing.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "buildings")
@Data
public class Building {
    @Id
    private String id;
    private String name;
    private String type;
    private String region; // e.g., "EMEA", "AMER", "APAC"
}
