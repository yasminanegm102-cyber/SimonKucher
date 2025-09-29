package com.example.pricing.repository;

import com.example.pricing.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, String> {}


