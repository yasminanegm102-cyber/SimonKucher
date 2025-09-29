package com.example.pricing.config;

import com.example.pricing.model.*;
import com.example.pricing.repository.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
public class SampleDataLoader implements CommandLineRunner {
    private final BuildingRepository buildingRepository;
    private final ProductRepository productRepository;
    private final PriceRepository priceRepository;
    private final BookingRepository bookingRepository;

    public SampleDataLoader(BuildingRepository buildingRepository,
                            ProductRepository productRepository,
                            PriceRepository priceRepository,
                            BookingRepository bookingRepository) {
        this.buildingRepository = buildingRepository;
        this.productRepository = productRepository;
        this.priceRepository = priceRepository;
        this.bookingRepository = bookingRepository;
    }

    @Override
    public void run(String... args) {
        if (buildingRepository.count() > 0 || productRepository.count() > 0) {
            return;
        }

        // Buildings
        Building b1 = new Building();
        b1.setId("B1");
        b1.setName("Building 1");
        b1.setType("City");

        Building b2 = new Building();
        b2.setId("B2");
        b2.setName("Building 2");
        b2.setType("Resort");

        buildingRepository.saveAll(List.of(b1, b2));

        // Products (Rooms)
        Product pS4 = new Product();
        pS4.setId("RoomS4");
        pS4.setBuildingId("B1");
        pS4.setRoomName("Room S4");
        pS4.setArrivalDate(LocalDate.of(2025, 5, 10));
        pS4.setNoOfBeds(2);
        pS4.setRoomType("Double");
        pS4.setGrade(2);
        pS4.setPrivatePool(true);

        Product pL = new Product();
        pL.setId("RoomL");
        pL.setBuildingId("B1");
        pL.setRoomName("Room L");
        pL.setArrivalDate(LocalDate.of(2025, 5, 12));
        pL.setNoOfBeds(3);
        pL.setRoomType("Suite");
        pL.setGrade(4);
        pL.setPrivatePool(false);

        Product pT = new Product();
        pT.setId("RoomT");
        pT.setBuildingId("B2");
        pT.setRoomName("Room T");
        pT.setArrivalDate(LocalDate.of(2025, 6, 5));
        pT.setNoOfBeds(2);
        pT.setRoomType("Single");
        pT.setGrade(3);
        pT.setPrivatePool(false);

        productRepository.saveAll(List.of(pS4, pL, pT));

        // Prices (multi-currency)
        Price ps4Usd = new Price();
        ps4Usd.setId(new PriceId("RoomS4", "USD"));
        ps4Usd.setValue(new BigDecimal("120"));
        ps4Usd.setLastUpdated(LocalDateTime.now());

        Price ps4Eur = new Price();
        ps4Eur.setId(new PriceId("RoomS4", "EUR"));
        ps4Eur.setValue(new BigDecimal("110"));
        ps4Eur.setLastUpdated(LocalDateTime.now());

        Price pLUsd = new Price();
        pLUsd.setId(new PriceId("RoomL", "USD"));
        pLUsd.setValue(new BigDecimal("140"));
        pLUsd.setLastUpdated(LocalDateTime.now());

        Price pTEgp = new Price();
        pTEgp.setId(new PriceId("RoomT", "EGP"));
        pTEgp.setValue(new BigDecimal("3800"));
        pTEgp.setLastUpdated(LocalDateTime.now());

        priceRepository.saveAll(List.of(ps4Usd, ps4Eur, pLUsd, pTEgp));

        // Bookings (for dashboard and algorithm)
        Booking bkg1 = new Booking();
        bkg1.setId("BKG1");
        bkg1.setProductId("RoomS4");
        bkg1.setArrivalDate(LocalDate.now());
        bkg1.setNights(2);
        bkg1.setPricePaid(118.0);

        Booking bkg2 = new Booking();
        bkg2.setId("BKG2");
        bkg2.setProductId("RoomS4");
        bkg2.setArrivalDate(LocalDate.now().plusDays(1));
        bkg2.setNights(1);
        bkg2.setPricePaid(122.0);

        Booking bkg3 = new Booking();
        bkg3.setId("BKG3");
        bkg3.setProductId("RoomL");
        bkg3.setArrivalDate(LocalDate.now());
        bkg3.setNights(3);
        bkg3.setPricePaid(150.0);

        bookingRepository.saveAll(List.of(bkg1, bkg2, bkg3));
    }
}


