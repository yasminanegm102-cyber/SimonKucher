package com.example.pricing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    private String id;
    private String buildingId;
    private String roomName;
    private LocalDate arrivalDate;
    private Integer noOfBeds;
    private String roomType;
    private Integer grade;
    private Boolean privatePool;
    private String productGroup; // e.g., "LUXURY", "BUDGET", "FAMILY"
}
