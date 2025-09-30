package com.example.pricing.controller;

import com.example.pricing.model.Building;
import com.example.pricing.repository.BuildingRepository;
import com.example.pricing.repository.ProductRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/filters")
public class FilterController {
    private final BuildingRepository buildingRepository;
    private final ProductRepository productRepository;

    public FilterController(BuildingRepository buildingRepository, ProductRepository productRepository) {
        this.buildingRepository = buildingRepository;
        this.productRepository = productRepository;
    }

    @GetMapping("/{clientId}")
    public Map<String, Object> getFilters(@PathVariable String clientId) {
        // Optional: scope by clientId if schema supports it; currently global
        List<Map<String, Object>> buildings = buildingRepository.findAll().stream()
                .map(b -> (Map<String, Object>) (Map) Map.of("id", b.getId(), "name", b.getName()))
                .collect(Collectors.toList());

        List<String> roomTypes = productRepository.findDistinctRoomTypes();
        List<Integer> beds = productRepository.findDistinctNoOfBeds();
        java.time.LocalDate minDate = productRepository.findMinArrivalDate();
        java.time.LocalDate maxDate = productRepository.findMaxArrivalDate();

        List<String> buildingTypes = buildingRepository.findAll().stream()
                .map(b -> b.getType())
                .filter(t -> t != null && !t.isBlank())
                .distinct()
                .collect(Collectors.toList());

        List<String> regions = buildingRepository.findAll().stream()
                .map(Building::getRegion)
                .filter(r -> r != null && !r.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        List<String> productGroups = productRepository.findAll().stream()
                .map(p -> p.getProductGroup())
                .filter(pg -> pg != null && !pg.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());

        return Map.of(
                "buildings", buildings,
                "roomTypes", roomTypes,
                "beds", beds,
                "arrivalDateRange", Map.of("min", minDate, "max", maxDate),
                "buildingTypes", buildingTypes,
                "regions", regions,
                "productGroups", productGroups
        );
    }
}
