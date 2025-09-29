package com.example.pricing.controller;

import com.example.pricing.repository.BookingRepository;
import com.example.pricing.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/metrics")
public class MetricsController {
    private final BookingRepository bookingRepository;
    private final ProductRepository productRepository;

    public MetricsController(BookingRepository bookingRepository, ProductRepository productRepository) {
        this.bookingRepository = bookingRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/occupancy")
    public Map<String, Object> occupancy(
            @RequestParam String buildingId,
            @RequestParam String startDate,
            @RequestParam String endDate
    ) {
        // naive occupancy proxy: bookings / products for date range
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        List<com.example.pricing.model.Product> products = productRepository.findByBuildingId(buildingId);
        List<String> productIds = products.stream().map(com.example.pricing.model.Product::getId).toList();
        long bookingCount = bookingRepository.findByProductIdIn(productIds).stream()
                .filter(b -> b.getArrivalDate() != null && !b.getArrivalDate().isBefore(start) && !b.getArrivalDate().isAfter(end))
                .count();
        int productCount = products.size();
        double occupancy = productCount == 0 ? 0.0 : (double) bookingCount / (double) productCount;
        return Map.of(
                "buildingId", buildingId,
                "startDate", start,
                "endDate", end,
                "bookingCount", bookingCount,
                "products", productCount,
                "occupancy", occupancy
        );
    }
}




