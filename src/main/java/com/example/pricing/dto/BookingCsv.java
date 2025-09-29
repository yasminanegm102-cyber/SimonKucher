package com.example.pricing.dto;

import lombok.Data;

@Data
public class BookingCsv {
    private String id;
    private String productId;
    private String arrivalDate;
    private String nights;
    private String pricePaid;
}
