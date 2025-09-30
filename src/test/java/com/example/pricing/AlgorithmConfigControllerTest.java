package com.example.pricing;

import com.example.pricing.controller.AlgorithmConfigController;
import com.example.pricing.model.User;
import com.example.pricing.repository.UserRepository;
import com.example.pricing.service.AlgorithmConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlgorithmConfigControllerTest {

    private AlgorithmConfigService algorithmConfigService;
    private UserRepository userRepository;
    private AlgorithmConfigController controller;

    @BeforeEach
    void setUp() {
        algorithmConfigService = mock(AlgorithmConfigService.class);
        userRepository = mock(UserRepository.class);
        controller = new AlgorithmConfigController(algorithmConfigService, userRepository);
    }

    @Test
    void testGetConfig() {
        // Arrange
        when(algorithmConfigService.getTargetOccupancy()).thenReturn(BigDecimal.valueOf(0.8));
        when(algorithmConfigService.getSensitivity()).thenReturn(BigDecimal.valueOf(0.25));
        when(algorithmConfigService.getWindowDays()).thenReturn(30);

        // Act
        Map<String, Object> config = controller.getConfig();

        // Assert
        assertEquals(BigDecimal.valueOf(0.8), config.get("targetOccupancy"));
        assertEquals(BigDecimal.valueOf(0.25), config.get("sensitivity"));
        assertEquals(30, config.get("windowDays"));
    }

    @Test
    void testUpdateConfig_AdminUser_Success() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85);
        body.put("sensitivity", 0.3);
        body.put("windowDays", 45);

        when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(algorithmConfigService.getTargetOccupancy()).thenReturn(BigDecimal.valueOf(0.85));
        when(algorithmConfigService.getSensitivity()).thenReturn(BigDecimal.valueOf(0.3));
        when(algorithmConfigService.getWindowDays()).thenReturn(45);

        // Act
        Map<String, Object> result = controller.updateConfig(body, "ADMIN001");

        // Assert
        assertNotNull(result);
        verify(algorithmConfigService, times(1)).update(
            eq(BigDecimal.valueOf(0.85)),
            eq(BigDecimal.valueOf(0.3)),
            eq(45)
        );
    }

    @Test
    void testUpdateConfig_NoUserId_ThrowsUnauthorized() {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.updateConfig(body, null)
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("X-User-Id header is required"));
        verify(algorithmConfigService, never()).update(any(), any(), any());
    }

    @Test
    void testUpdateConfig_EmptyUserId_ThrowsUnauthorized() {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.updateConfig(body, "")
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("X-User-Id header is required"));
    }

    @Test
    void testUpdateConfig_InvalidUser_ThrowsUnauthorized() {
        // Arrange
        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85);

        when(userRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.updateConfig(body, "INVALID")
        );
        
        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Invalid user"));
        verify(algorithmConfigService, never()).update(any(), any(), any());
    }

    @Test
    void testUpdateConfig_NonAdminUser_ThrowsForbidden() {
        // Arrange
        User regionalManager = new User();
        regionalManager.setId("RM_001");
        regionalManager.setRole("REGIONAL_MANAGER");

        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85);

        when(userRepository.findById("RM_001")).thenReturn(Optional.of(regionalManager));

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.updateConfig(body, "RM_001")
        );
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("Only ADMIN users can update algorithm configuration"));
        assertTrue(exception.getReason().contains("Your role: REGIONAL_MANAGER"));
        verify(algorithmConfigService, never()).update(any(), any(), any());
    }

    @Test
    void testUpdateConfig_PricingManager_ThrowsForbidden() {
        // Arrange
        User pricingManager = new User();
        pricingManager.setId("PM_001");
        pricingManager.setRole("PRICING_MANAGER");

        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85);

        when(userRepository.findById("PM_001")).thenReturn(Optional.of(pricingManager));

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> controller.updateConfig(body, "PM_001")
        );
        
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        verify(algorithmConfigService, never()).update(any(), any(), any());
    }

    @Test
    void testUpdateConfig_AdminCaseInsensitive_Success() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("admin"); // lowercase

        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85);

        when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(algorithmConfigService.getTargetOccupancy()).thenReturn(BigDecimal.valueOf(0.85));
        when(algorithmConfigService.getSensitivity()).thenReturn(BigDecimal.valueOf(0.25));
        when(algorithmConfigService.getWindowDays()).thenReturn(30);

        // Act
        Map<String, Object> result = controller.updateConfig(body, "ADMIN001");

        // Assert
        assertNotNull(result);
        verify(algorithmConfigService, times(1)).update(any(), any(), any());
    }

    @Test
    void testUpdateConfig_PartialUpdate_Success() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        Map<String, Object> body = new HashMap<>();
        body.put("targetOccupancy", 0.85); // Only update one parameter

        when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(algorithmConfigService.getTargetOccupancy()).thenReturn(BigDecimal.valueOf(0.85));
        when(algorithmConfigService.getSensitivity()).thenReturn(BigDecimal.valueOf(0.25));
        when(algorithmConfigService.getWindowDays()).thenReturn(30);

        // Act
        Map<String, Object> result = controller.updateConfig(body, "ADMIN001");

        // Assert
        assertNotNull(result);
        verify(algorithmConfigService, times(1)).update(
            eq(BigDecimal.valueOf(0.85)),
            isNull(), // sensitivity not provided
            isNull()  // windowDays not provided
        );
    }

    @Test
    void testUpdateConfig_EmptyBody_Success() {
        // Arrange
        User admin = new User();
        admin.setId("ADMIN001");
        admin.setRole("ADMIN");

        Map<String, Object> body = new HashMap<>(); // Empty body

        when(userRepository.findById("ADMIN001")).thenReturn(Optional.of(admin));
        when(algorithmConfigService.getTargetOccupancy()).thenReturn(BigDecimal.valueOf(0.8));
        when(algorithmConfigService.getSensitivity()).thenReturn(BigDecimal.valueOf(0.25));
        when(algorithmConfigService.getWindowDays()).thenReturn(30);

        // Act
        Map<String, Object> result = controller.updateConfig(body, "ADMIN001");

        // Assert
        assertNotNull(result);
        verify(algorithmConfigService, times(1)).update(isNull(), isNull(), isNull());
    }
}
