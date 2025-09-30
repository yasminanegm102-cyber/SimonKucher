package com.example.pricing;

import com.example.pricing.model.*;
import com.example.pricing.repository.*;
import com.example.pricing.service.ConfirmationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ConfirmationServiceTest {

    private PriceConfirmationRepository confirmationRepo;
    private PriceRecommendationRepository recommendationRepo;
    private ProductRepository productRepo;
    private BuildingRepository buildingRepo;
    private UserRepository userRepo;
    private ConfirmationService confirmationService;

    @BeforeEach
    void setUp() {
        confirmationRepo = mock(PriceConfirmationRepository.class);
        recommendationRepo = mock(PriceRecommendationRepository.class);
        productRepo = mock(ProductRepository.class);
        buildingRepo = mock(BuildingRepository.class);
        userRepo = mock(UserRepository.class);
        
        confirmationService = new ConfirmationService(
            confirmationRepo, 
            recommendationRepo, 
            productRepo, 
            buildingRepo, 
            userRepo
        );
    }

    @Test
    void testConfirm_AdminUser_Success() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        when(userRepo.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(confirmationRepo.save(any(PriceConfirmation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PriceConfirmation result = confirmationService.confirm(
            "P001", "ACCEPT", BigDecimal.valueOf(100), "USD", "ADMIN001"
        );

        // Assert
        assertNotNull(result);
        assertEquals("P001", result.getProductId());
        assertEquals("ACCEPT", result.getAction());
        assertEquals(BigDecimal.valueOf(100), result.getConfirmedValue());
        assertEquals("USD", result.getCurrency());
        assertEquals("ADMIN001", result.getUserId());
        assertFalse(result.getSynced());
        verify(confirmationRepo, times(1)).save(any(PriceConfirmation.class));
    }

    @Test
    void testConfirm_RegionalManager_SameRegion_Success() {
        // Arrange
        User regionalManager = new User();
        regionalManager.setId("RM_EMEA_001");
        regionalManager.setRole("REGIONAL_MANAGER");
        regionalManager.setRegion("EMEA");

        Product product = new Product();
        product.setId("P001");
        product.setBuildingId("B001");

        Building building = new Building();
        building.setId("B001");
        building.setRegion("EMEA");

        when(userRepo.findById("RM_EMEA_001")).thenReturn(Optional.of(regionalManager));
        when(productRepo.findById("P001")).thenReturn(Optional.of(product));
        when(buildingRepo.findById("B001")).thenReturn(Optional.of(building));
        when(confirmationRepo.save(any(PriceConfirmation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PriceConfirmation result = confirmationService.confirm(
            "P001", "ACCEPT", BigDecimal.valueOf(100), "USD", "RM_EMEA_001"
        );

        // Assert
        assertNotNull(result);
        assertEquals("P001", result.getProductId());
        verify(confirmationRepo, times(1)).save(any(PriceConfirmation.class));
    }

    @Test
    void testConfirm_RegionalManager_DifferentRegion_ThrowsException() {
        // Arrange
        User regionalManager = new User();
        regionalManager.setId("RM_EMEA_001");
        regionalManager.setRole("REGIONAL_MANAGER");
        regionalManager.setRegion("EMEA");

        Product product = new Product();
        product.setId("P001");
        product.setBuildingId("B001");

        Building building = new Building();
        building.setId("B001");
        building.setRegion("APAC");  // Different region!

        when(userRepo.findById("RM_EMEA_001")).thenReturn(Optional.of(regionalManager));
        when(productRepo.findById("P001")).thenReturn(Optional.of(product));
        when(buildingRepo.findById("B001")).thenReturn(Optional.of(building));

        // Act & Assert
        SecurityException exception = assertThrows(
            SecurityException.class,
            () -> confirmationService.confirm("P001", "ACCEPT", BigDecimal.valueOf(100), "USD", "RM_EMEA_001")
        );
        
        assertTrue(exception.getMessage().contains("Regional manager can only confirm prices in their assigned region"));
        assertTrue(exception.getMessage().contains("User region: EMEA"));
        assertTrue(exception.getMessage().contains("Building region: APAC"));
        verify(confirmationRepo, never()).save(any(PriceConfirmation.class));
    }

    @Test
    void testConfirm_PricingManager_AnyRegion_Success() {
        // Arrange
        User pricingManager = new User();
        pricingManager.setId("PM_001");
        pricingManager.setRole("PRICING_MANAGER");

        Product product = new Product();
        product.setId("P001");
        product.setBuildingId("B001");

        Building building = new Building();
        building.setId("B001");
        building.setRegion("APAC");

        when(userRepo.findById("PM_001")).thenReturn(Optional.of(pricingManager));
        when(confirmationRepo.save(any(PriceConfirmation.class))).thenAnswer(i -> i.getArgument(0));

        // Act
        PriceConfirmation result = confirmationService.confirm(
            "P001", "ACCEPT", BigDecimal.valueOf(100), "USD", "PM_001"
        );

        // Assert
        assertNotNull(result);
        assertEquals("P001", result.getProductId());
        verify(confirmationRepo, times(1)).save(any(PriceConfirmation.class));
        // Should not check building/region for PRICING_MANAGER
        verify(productRepo, never()).findById(any());
        verify(buildingRepo, never()).findById(any());
    }

    @Test
    void testConfirm_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepo.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> confirmationService.confirm("P001", "ACCEPT", BigDecimal.valueOf(100), "USD", "INVALID")
        );
        
        assertTrue(exception.getMessage().contains("User not found"));
    }

    @Test
    void testConfirm_Override_WithinBounds_Success() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        PriceRecommendation lastRec = new PriceRecommendation();
        lastRec.setProductId("P001");
        lastRec.setRecommendedValue(BigDecimal.valueOf(100));
        lastRec.setRecommendedAt(LocalDateTime.now());

        when(userRepo.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(recommendationRepo.findAll()).thenReturn(Arrays.asList(lastRec));
        when(confirmationRepo.save(any(PriceConfirmation.class))).thenAnswer(i -> i.getArgument(0));

        // Act - Override with 120 (within ±30% of 100: 70-130)
        PriceConfirmation result = confirmationService.confirm(
            "P001", "OVERRIDE", BigDecimal.valueOf(120), "USD", "ADMIN001"
        );

        // Assert
        assertNotNull(result);
        assertEquals("OVERRIDE", result.getAction());
        assertEquals(BigDecimal.valueOf(120), result.getConfirmedValue());
        verify(confirmationRepo, times(1)).save(any(PriceConfirmation.class));
    }

    @Test
    void testConfirm_Override_BelowMinBound_ThrowsException() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        PriceRecommendation lastRec = new PriceRecommendation();
        lastRec.setProductId("P001");
        lastRec.setRecommendedValue(BigDecimal.valueOf(100));
        lastRec.setRecommendedAt(LocalDateTime.now());

        when(userRepo.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(recommendationRepo.findAll()).thenReturn(Arrays.asList(lastRec));

        // Act & Assert - Override with 65 (below 70% of 100)
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> confirmationService.confirm("P001", "OVERRIDE", BigDecimal.valueOf(65), "USD", "ADMIN001")
        );
        
        assertTrue(exception.getMessage().contains("Override out of allowed bounds"));
        verify(confirmationRepo, never()).save(any(PriceConfirmation.class));
    }

    @Test
    void testConfirm_Override_AboveMaxBound_ThrowsException() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        PriceRecommendation lastRec = new PriceRecommendation();
        lastRec.setProductId("P001");
        lastRec.setRecommendedValue(BigDecimal.valueOf(100));
        lastRec.setRecommendedAt(LocalDateTime.now());

        when(userRepo.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(recommendationRepo.findAll()).thenReturn(Arrays.asList(lastRec));

        // Act & Assert - Override with 135 (above 130% of 100)
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> confirmationService.confirm("P001", "OVERRIDE", BigDecimal.valueOf(135), "USD", "ADMIN001")
        );
        
        assertTrue(exception.getMessage().contains("Override out of allowed bounds"));
        verify(confirmationRepo, never()).save(any(PriceConfirmation.class));
    }

    @Test
    void testConfirm_Accept_NoOverrideValidation() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        when(userRepo.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(confirmationRepo.save(any(PriceConfirmation.class))).thenAnswer(i -> i.getArgument(0));

        // Act - ACCEPT action should not trigger override validation
        PriceConfirmation result = confirmationService.confirm(
            "P001", "ACCEPT", BigDecimal.valueOf(100), "USD", "ADMIN001"
        );

        // Assert
        assertNotNull(result);
        assertEquals("ACCEPT", result.getAction());
        verify(recommendationRepo, never()).findAll(); // Should not check recommendations
        verify(confirmationRepo, times(1)).save(any(PriceConfirmation.class));
    }

    @Test
    void testConfirm_Reject_NoOverrideValidation() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        when(userRepo.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(confirmationRepo.save(any(PriceConfirmation.class))).thenAnswer(i -> i.getArgument(0));

        // Act - REJECT action should not trigger override validation
        PriceConfirmation result = confirmationService.confirm(
            "P001", "REJECT", null, "USD", "ADMIN001"
        );

        // Assert
        assertNotNull(result);
        assertEquals("REJECT", result.getAction());
        assertNull(result.getConfirmedValue());
        verify(recommendationRepo, never()).findAll(); // Should not check recommendations
        verify(confirmationRepo, times(1)).save(any(PriceConfirmation.class));
    }
}
