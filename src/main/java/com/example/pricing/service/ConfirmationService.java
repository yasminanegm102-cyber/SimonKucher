package com.example.pricing.service;

import com.example.pricing.model.PriceConfirmation;
import com.example.pricing.model.Product;
import com.example.pricing.model.Building;
import com.example.pricing.model.User;
import com.example.pricing.repository.PriceConfirmationRepository;
import com.example.pricing.repository.PriceRecommendationRepository;
import com.example.pricing.repository.ProductRepository;
import com.example.pricing.repository.BuildingRepository;
import com.example.pricing.repository.UserRepository;
import com.example.pricing.model.PriceRecommendation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class ConfirmationService {
    private final PriceConfirmationRepository repo;
    private final PriceRecommendationRepository recRepo;
    private final ProductRepository productRepo;
    private final BuildingRepository buildingRepo;
    private final UserRepository userRepo;

    public ConfirmationService(PriceConfirmationRepository repo, 
                              PriceRecommendationRepository recRepo,
                              ProductRepository productRepo,
                              BuildingRepository buildingRepo,
                              UserRepository userRepo) {
        this.repo = repo;
        this.recRepo = recRepo;
        this.productRepo = productRepo;
        this.buildingRepo = buildingRepo;
        this.userRepo = userRepo;
    }

    @Transactional
    public PriceConfirmation confirm(String productId, String action, BigDecimal value, String currency, String userId) {
        // Enforce regional restrictions for regional managers
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        
        if ("REGIONAL_MANAGER".equalsIgnoreCase(user.getRole())) {
            Product product = productRepo.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
            Building building = buildingRepo.findById(product.getBuildingId())
                    .orElseThrow(() -> new IllegalArgumentException("Building not found: " + product.getBuildingId()));
            
            if (user.getRegion() != null && building.getRegion() != null 
                    && !user.getRegion().equalsIgnoreCase(building.getRegion())) {
                throw new SecurityException("Regional manager can only confirm prices in their assigned region. User region: " 
                        + user.getRegion() + ", Building region: " + building.getRegion());
            }
        }
        
        // Enforce override eligibility: within ±30% of last recommendation, if present
        if ("OVERRIDE".equalsIgnoreCase(action) && value != null) {
            BigDecimal last = findLastRecommendation(productId);
            if (last != null) {
                BigDecimal min = last.multiply(BigDecimal.valueOf(0.7));
                BigDecimal max = last.multiply(BigDecimal.valueOf(1.3));
                if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                    throw new IllegalArgumentException("Override out of allowed bounds (±30% of recommended)");
                }
            }
        }
        PriceConfirmation pc = new PriceConfirmation();
        pc.setProductId(productId);
        pc.setAction(action);
        pc.setConfirmedValue(value);
        pc.setCurrency(currency);
        pc.setUserId(userId);
        pc.setConfirmedAt(LocalDateTime.now());
        pc.setSynced(false);
        return repo.save(pc);
    }

    private BigDecimal findLastRecommendation(String productId) {
        return recRepo.findAll().stream()
                .filter(r -> productId.equals(r.getProductId()))
                .sorted(java.util.Comparator.comparing(PriceRecommendation::getRecommendedAt).reversed())
                .map(PriceRecommendation::getRecommendedValue)
                .findFirst()
                .orElse(null);
    }
}
