package com.example.pricing.repository;

import com.example.pricing.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.time.LocalDate;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByArrivalDateAndRoomTypeAndNoOfBedsAndGradeAndPrivatePool(
            LocalDate arrivalDate, String roomType, Integer noOfBeds, Integer grade, Boolean privatePool);
    List<Product> findByBuildingId(String buildingId);

    @Query("select distinct p.roomType from Product p")
    List<String> findDistinctRoomTypes();

    @Query("select distinct p.noOfBeds from Product p")
    List<Integer> findDistinctNoOfBeds();

    @Query("select min(p.arrivalDate) from Product p")
    LocalDate findMinArrivalDate();

    @Query("select max(p.arrivalDate) from Product p")
    LocalDate findMaxArrivalDate();
}
