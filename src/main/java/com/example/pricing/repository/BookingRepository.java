package com.example.pricing.repository;

import com.example.pricing.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, String> {
    List<Booking> findByProductIdIn(List<String> productIds);
}
