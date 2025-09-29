package com.example.pricing.repository;

import com.example.pricing.model.Price;
import com.example.pricing.model.PriceId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceRepository extends JpaRepository<Price, PriceId> {
    List<Price> findByIdProductId(String productId);
    List<Price> findByIdCurrency(String currency);
    Page<Price> findAll(Pageable pageable);
    Page<Price> findByIdCurrency(String currency, Pageable pageable);
    List<Price> findByIdProductIdAndIdCurrency(String productId, String currency);
}
