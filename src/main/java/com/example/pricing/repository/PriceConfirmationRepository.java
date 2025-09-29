package com.example.pricing.repository;

import com.example.pricing.model.PriceConfirmation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceConfirmationRepository extends JpaRepository<PriceConfirmation, Long> {
    List<PriceConfirmation> findBySyncedFalse();
}
