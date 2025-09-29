package com.example.pricing.repository;

import com.example.pricing.model.Building;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BuildingRepository extends JpaRepository<Building, String> {}
