package com.example.pricing;

import com.example.pricing.controller.RecommendationController;
import com.example.pricing.model.Building;
import com.example.pricing.model.Price;
import com.example.pricing.model.PriceId;
import com.example.pricing.model.Product;
import com.example.pricing.repository.BuildingRepository;
import com.example.pricing.repository.PriceRepository;
import com.example.pricing.repository.ProductRepository;
import com.example.pricing.service.ClusteringService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RecommendationControllerTest {

    private BuildingRepository buildingRepo;
    private ProductRepository productRepo;
    private PriceRepository priceRepo;
    private ClusteringService clusteringService;
    private RecommendationController controller;

    @BeforeEach
    void setUp() {
        buildingRepo = mock(BuildingRepository.class);
        productRepo = mock(ProductRepository.class);
        priceRepo = mock(PriceRepository.class);
        clusteringService = mock(ClusteringService.class);
        controller = new RecommendationController(buildingRepo, productRepo, priceRepo, clusteringService);
    }

    @Test
    void testGetGrouped_NoFilters_ReturnsAllBuildings() {
        // Arrange
        Building building1 = new Building();
        building1.setId("B001");
        building1.setName("Building 1");
        building1.setRegion("EMEA");

        Building building2 = new Building();
        building2.setId("B002");
        building2.setName("Building 2");
        building2.setRegion("APAC");

        Product product1 = new Product();
        product1.setId("P001");
        product1.setRoomName("Room 101");
        product1.setNoOfBeds(2);
        product1.setRoomType("Standard");
        product1.setProductGroup("BUDGET");

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building1, building2));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList(product1));
        when(productRepo.findByBuildingId("B002")).thenReturn(Arrays.asList());
        when(priceRepo.findByIdProductId("P001")).thenReturn(Arrays.asList());

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, null, null, null, null, null, null
        );

        // Assert
        assertEquals(2, result.size());
        assertEquals("B001", result.get(0).getId());
        assertEquals("B002", result.get(1).getId());
    }

    @Test
    void testGetGrouped_FilterByRegion_ReturnsOnlyMatchingBuildings() {
        // Arrange
        Building building1 = new Building();
        building1.setId("B001");
        building1.setName("Building 1");
        building1.setRegion("EMEA");

        Building building2 = new Building();
        building2.setId("B002");
        building2.setName("Building 2");
        building2.setRegion("APAC");

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building1, building2));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList());

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, null, null, null, null, null, "EMEA"
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals("B001", result.get(0).getId());
        assertEquals("Building 1", result.get(0).getName());
    }

    @Test
    void testGetGrouped_FilterByProductGroup_ReturnsMatchingProducts() {
        // Arrange
        Building building = new Building();
        building.setId("B001");
        building.setName("Building 1");

        Product product1 = new Product();
        product1.setId("P001");
        product1.setRoomName("Budget Room");
        product1.setProductGroup("BUDGET");
        product1.setNoOfBeds(2);
        product1.setRoomType("Standard");

        Product product2 = new Product();
        product2.setId("P002");
        product2.setRoomName("Luxury Suite");
        product2.setProductGroup("LUXURY");
        product2.setNoOfBeds(3);
        product2.setRoomType("Suite");

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList(product1, product2));
        when(priceRepo.findByIdProductId("P001")).thenReturn(Arrays.asList());

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, null, null, null, null, "BUDGET", null
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getProducts().size());
        assertEquals("P001", result.get(0).getProducts().get(0).getProductId());
    }

    @Test
    void testGetGrouped_FilterByRoomType_ReturnsMatchingProducts() {
        // Arrange
        Building building = new Building();
        building.setId("B001");
        building.setName("Building 1");

        Product product1 = new Product();
        product1.setId("P001");
        product1.setRoomType("Standard");
        product1.setNoOfBeds(2);

        Product product2 = new Product();
        product2.setId("P002");
        product2.setRoomType("Suite");
        product2.setNoOfBeds(3);

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList(product1, product2));
        when(priceRepo.findByIdProductId("P001")).thenReturn(Arrays.asList());

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, "Standard", null, null, null, null, null
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getProducts().size());
        assertEquals("P001", result.get(0).getProducts().get(0).getProductId());
    }

    @Test
    void testGetGrouped_FilterByBeds_ReturnsMatchingProducts() {
        // Arrange
        Building building = new Building();
        building.setId("B001");
        building.setName("Building 1");

        Product product1 = new Product();
        product1.setId("P001");
        product1.setNoOfBeds(2);
        product1.setRoomType("Standard");

        Product product2 = new Product();
        product2.setId("P002");
        product2.setNoOfBeds(4);
        product2.setRoomType("Suite");

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList(product1, product2));
        when(priceRepo.findByIdProductId("P001")).thenReturn(Arrays.asList());

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, null, 2, null, null, null, null
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getProducts().size());
        assertEquals(2, result.get(0).getProducts().get(0).getBeds());
    }

    @Test
    void testGetGrouped_FilterByDateRange_ReturnsMatchingProducts() {
        // Arrange
        Building building = new Building();
        building.setId("B001");
        building.setName("Building 1");

        Product product1 = new Product();
        product1.setId("P001");
        product1.setArrivalDate(LocalDate.of(2025, 5, 15));
        product1.setNoOfBeds(2);
        product1.setRoomType("Standard");

        Product product2 = new Product();
        product2.setId("P002");
        product2.setArrivalDate(LocalDate.of(2025, 7, 20));
        product2.setNoOfBeds(2);
        product2.setRoomType("Standard");

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList(product1, product2));
        when(priceRepo.findByIdProductId("P001")).thenReturn(Arrays.asList());

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, null, null, "2025-05-01", "2025-05-31", null, null
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getProducts().size());
        assertEquals("P001", result.get(0).getProducts().get(0).getProductId());
    }

    @Test
    void testGetGrouped_MultipleFilters_ReturnsMatchingProducts() {
        // Arrange
        Building building1 = new Building();
        building1.setId("B001");
        building1.setName("Building 1");
        building1.setRegion("EMEA");

        Building building2 = new Building();
        building2.setId("B002");
        building2.setName("Building 2");
        building2.setRegion("APAC");

        Product product1 = new Product();
        product1.setId("P001");
        product1.setRoomType("Suite");
        product1.setNoOfBeds(2);
        product1.setProductGroup("LUXURY");
        product1.setArrivalDate(LocalDate.of(2025, 5, 15));

        Product product2 = new Product();
        product2.setId("P002");
        product2.setRoomType("Standard");
        product2.setNoOfBeds(2);
        product2.setProductGroup("BUDGET");

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building1, building2));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList(product1, product2));
        when(priceRepo.findByIdProductId("P001")).thenReturn(Arrays.asList());

        // Act - Filter by region=EMEA, productGroup=LUXURY, roomType=Suite
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, "Suite", null, null, null, "LUXURY", "EMEA"
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals("B001", result.get(0).getId());
        assertEquals(1, result.get(0).getProducts().size());
        assertEquals("P001", result.get(0).getProducts().get(0).getProductId());
    }

    @Test
    void testGetGrouped_WithMultiCurrencyPrices_ReturnsCorrectPriceMap() {
        // Arrange
        Building building = new Building();
        building.setId("B001");
        building.setName("Building 1");

        Product product = new Product();
        product.setId("P001");
        product.setRoomName("Room 101");
        product.setNoOfBeds(2);
        product.setRoomType("Standard");

        Price priceUSD = new Price();
        priceUSD.setId(new PriceId("P001", "USD"));
        priceUSD.setValue(BigDecimal.valueOf(100));

        Price priceEUR = new Price();
        priceEUR.setId(new PriceId("P001", "EUR"));
        priceEUR.setValue(BigDecimal.valueOf(90));

        Price priceGBP = new Price();
        priceGBP.setId(new PriceId("P001", "GBP"));
        priceGBP.setValue(BigDecimal.valueOf(80));

        when(buildingRepo.findAll()).thenReturn(Arrays.asList(building));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList(product));
        when(priceRepo.findByIdProductId("P001")).thenReturn(Arrays.asList(priceUSD, priceEUR, priceGBP));

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            null, null, null, null, null, null, null
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).getProducts().size());
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> prices = (java.util.Map<String, Object>) result.get(0).getProducts().get(0).getPrices();
        assertEquals(3, prices.size());
        assertEquals(BigDecimal.valueOf(100), prices.get("USD"));
        assertEquals(BigDecimal.valueOf(90), prices.get("EUR"));
        assertEquals(BigDecimal.valueOf(80), prices.get("GBP"));
    }

    @Test
    void testGetGrouped_FilterBySpecificBuildingIds_ReturnsOnlyThoseBuildings() {
        // Arrange
        Building building1 = new Building();
        building1.setId("B001");
        building1.setName("Building 1");

        Building building2 = new Building();
        building2.setId("B002");
        building2.setName("Building 2");

        when(buildingRepo.findAllById(Arrays.asList("B001"))).thenReturn(Arrays.asList(building1));
        when(productRepo.findByBuildingId("B001")).thenReturn(Arrays.asList());

        // Act
        List<RecommendationController.BuildingDto> result = controller.getGrouped(
            Arrays.asList("B001"), null, null, null, null, null, null
        );

        // Assert
        assertEquals(1, result.size());
        assertEquals("B001", result.get(0).getId());
        verify(buildingRepo, never()).findAll();
    }
}
