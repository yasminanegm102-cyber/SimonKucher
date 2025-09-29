package com.example.pricing.dto;

import lombok.Data;

@Data
public class ProductCsv {
    private String id;
    private String buildingId;
    private String roomName;
    private String arrivalDate;
    private String noOfBeds;
    private String roomType;
    private String grade;
    private String privatePool;
}
