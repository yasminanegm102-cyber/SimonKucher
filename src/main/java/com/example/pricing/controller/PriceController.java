package com.example.pricing.controller;

import com.example.pricing.model.Price;
import com.example.pricing.repository.PriceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/prices")
public class PriceController {
    @Autowired
    private PriceRepository priceRepository;

    // Multi-currency, sorting, and pagination endpoint
    @GetMapping("")
    public Page<Price> getPrices(
            @RequestParam(required = false) String currency,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "productId") String sortBy,
            @RequestParam(defaultValue = "asc") String order
    ) {
        Sort sort = order.equalsIgnoreCase("desc") ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        if (currency != null && !currency.isEmpty()) {
            return priceRepository.findByIdCurrency(currency, pageable);
        } else {
            return priceRepository.findAll(pageable);
        }
    }

    // Get all prices for a product in all currencies
    @GetMapping("/by-product")
    public List<Price> getPricesByProduct(@RequestParam String productId) {
        return priceRepository.findByIdProductId(productId);
    }
}
