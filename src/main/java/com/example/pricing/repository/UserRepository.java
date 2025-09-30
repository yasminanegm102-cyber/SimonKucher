package com.example.pricing.repository;

import com.example.pricing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<User, String> {
    List<User> findByRole(String role);
    List<User> findByRegion(String region);
}




