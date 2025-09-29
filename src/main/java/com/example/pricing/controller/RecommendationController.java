package com.example.pricing.controller;

import com.example.pricing.model.Building;
import com.example.pricing.repository.BuildingRepository;
import com.example.pricing.repository.ProductRepository;
import com.example.pricing.repository.PriceRepository;
import com.example.pricing.model.Price;
import com.example.pricing.service.ClusteringService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/recommendations")

public class RecommendationController {
    private final BuildingRepository buildingRepo;
    private final ProductRepository productRepo;
    private final PriceRepository priceRepo;
    private final ClusteringService clusteringService;

    public RecommendationController(BuildingRepository buildingRepo, ProductRepository productRepo, PriceRepository priceRepo, ClusteringService clusteringService) {
        this.buildingRepo = buildingRepo;
        this.productRepo = productRepo;
        this.priceRepo = priceRepo;
        this.clusteringService = clusteringService;
    }

    @GetMapping("/grouped")
    public List<BuildingDto> getGrouped(
            @RequestParam(required = false) List<String> buildingIds,
            @RequestParam(required = false) String roomType,
            @RequestParam(required = false) Integer beds,
            @RequestParam(required = false) String arrivalFrom,
            @RequestParam(required = false) String arrivalTo
    ) {
        List<Building> buildings = (buildingIds == null || buildingIds.isEmpty())
                ? buildingRepo.findAll()
                : buildingRepo.findAllById(buildingIds);

        java.time.LocalDate fromDate = arrivalFrom == null || arrivalFrom.isBlank() ? null : java.time.LocalDate.parse(arrivalFrom);
        java.time.LocalDate toDate = arrivalTo == null || arrivalTo.isBlank() ? null : java.time.LocalDate.parse(arrivalTo);

        return buildings.stream().map(b -> {
            List<com.example.pricing.model.Product> base = productRepo.findByBuildingId(b.getId());
            List<com.example.pricing.model.Product> filtered = base.stream().filter(p -> {
                if (roomType != null && !roomType.isBlank() && !roomType.equals(p.getRoomType())) return false;
                if (beds != null && !beds.equals(p.getNoOfBeds())) return false;
                if (fromDate != null && (p.getArrivalDate() == null || p.getArrivalDate().isBefore(fromDate))) return false;
                if (toDate != null && (p.getArrivalDate() == null || p.getArrivalDate().isAfter(toDate))) return false;
                return true;
            }).toList();

            List<ProductDto> products = filtered.stream().map(p -> {
                // Get all prices for this product (multi-currency)
                List<Price> prices = priceRepo.findByIdProductId(p.getId());
                // Map currency to value
                java.util.Map<String, Object> priceMap = new java.util.HashMap<>();
                for (Price price : prices) {
                    String currency = price.getId().getCurrency();
                    priceMap.put(currency, price.getValue());
                }
                return new ProductDto(p.getId(), p.getRoomName(), p.getNoOfBeds(), p.getRoomType(), p.getArrivalDate(), priceMap);
            }).collect(Collectors.toList());
            return new BuildingDto(b.getId(), b.getName(), products);
        }).collect(Collectors.toList());
    }

    // New: retrieve bookings by cluster attributes
    @GetMapping("/bookings-by-cluster")
    public List<com.example.pricing.model.Booking> getBookingsByCluster(
            @RequestParam(required = false) java.time.LocalDate arrivalDate,
            @RequestParam String roomType,
            @RequestParam Integer noOfBeds,
            @RequestParam Integer grade,
            @RequestParam Boolean privatePool
    ) {
        return clusteringService.bookingsForCluster(arrivalDate, roomType, noOfBeds, grade, privatePool);
    }

    public static class BuildingDto {
        private String id;
        private String name;
        private List<ProductDto> products;

        public BuildingDto(String id, String name, List<ProductDto> products) {
            this.id = id;
            this.name = name;
            this.products = products;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public List<ProductDto> getProducts() { return products; }
    }

    public static class ProductDto {
        private String productId;
        private String roomName;
        private Integer beds;
        private String roomType;
        private java.time.LocalDate arrivalDate;
        private Object prices;

        public ProductDto(String productId, String roomName, Integer beds, String roomType, java.time.LocalDate arrivalDate, Object prices) {
            this.productId = productId;
            this.roomName = roomName;
            this.beds = beds;
            this.roomType = roomType;
            this.arrivalDate = arrivalDate;
            this.prices = prices;
        }

        public String getProductId() { return productId; }
        public String getRoomName() { return roomName; }
        public Integer getBeds() { return beds; }
        public String getRoomType() { return roomType; }
        public java.time.LocalDate getArrivalDate() { return arrivalDate; }
        public Object getPrices() { return prices; }
    }
}
