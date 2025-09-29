package com.example.pricing.service;

import com.example.pricing.model.Booking;
import com.example.pricing.model.Product;
import com.example.pricing.repository.BookingRepository;
import com.example.pricing.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClusteringService {
    private final ProductRepository productRepository;
    private final BookingRepository bookingRepository;

    public ClusteringService(ProductRepository productRepository, BookingRepository bookingRepository) {
        this.productRepository = productRepository;
        this.bookingRepository = bookingRepository;
    }

    public List<Booking> bookingsForCluster(LocalDate arrivalDate, String roomType, Integer beds, Integer grade, Boolean privatePool) {
        List<Product> products = productRepository.findByArrivalDateAndRoomTypeAndNoOfBedsAndGradeAndPrivatePool(
                arrivalDate, roomType, beds, grade, privatePool);
        if (products.isEmpty()) return List.of();
        List<String> productIds = products.stream().map(Product::getId).collect(Collectors.toList());
        return bookingRepository.findByProductIdIn(productIds);
    }
}
