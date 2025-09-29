package com.example.pricing.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "bookings")
@Data
public class Booking {
    @Id
    private String id;
    private String productId;
    private LocalDate arrivalDate;
    private Integer nights;
    private Double pricePaid;
}
