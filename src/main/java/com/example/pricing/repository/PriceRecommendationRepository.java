package com.example.pricing.repository;

import com.example.pricing.model.PriceRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRecommendationRepository extends JpaRepository<PriceRecommendation, Long> {
}
