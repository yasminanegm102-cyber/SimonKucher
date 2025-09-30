package com.example.pricing;

import com.example.pricing.controller.UserController;
import com.example.pricing.model.User;
import com.example.pricing.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserControllerTest {

    private UserRepository userRepository;
    private UserController userController;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        userController = new UserController(userRepository);
    }

    @Test
    void testGetAllUsers() {
        // Arrange
        User user1 = new User();
        user1.setId("U001");
        user1.setName("John Doe");
        user1.setRole("ADMIN");

        User user2 = new User();
        user2.setId("U002");
        user2.setName("Jane Smith");
        user2.setRole("REGIONAL_MANAGER");
        user2.setRegion("EMEA");

        when(userRepository.findAll()).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<User> users = userController.getAllUsers();

        // Assert
        assertEquals(2, users.size());
        assertEquals("U001", users.get(0).getId());
        assertEquals("U002", users.get(1).getId());
        verify(userRepository, times(1)).findAll();
    }

    @Test
    void testGetUserById_Success() {
        // Arrange
        User user = new User();
        user.setId("U001");
        user.setName("John Doe");
        user.setRole("ADMIN");

        when(userRepository.findById("U001")).thenReturn(Optional.of(user));

        // Act
        User result = userController.getUserById("U001");

        // Assert
        assertNotNull(result);
        assertEquals("U001", result.getId());
        assertEquals("John Doe", result.getName());
        verify(userRepository, times(1)).findById("U001");
    }

    @Test
    void testGetUserById_NotFound() {
        // Arrange
        when(userRepository.findById("INVALID")).thenReturn(Optional.empty());

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userController.getUserById("INVALID")
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User not found"));
    }

    @Test
    void testCreateUser_Success() {
        // Arrange
        User user = new User();
        user.setId("U001");
        user.setName("John Doe");
        user.setRole("ADMIN");

        when(userRepository.existsById("U001")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userController.createUser(user);

        // Assert
        assertNotNull(result);
        assertEquals("U001", result.getId());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testCreateUser_MissingId() {
        // Arrange
        User user = new User();
        user.setName("John Doe");
        user.setRole("ADMIN");

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userController.createUser(user)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User ID is required"));
    }

    @Test
    void testCreateUser_MissingName() {
        // Arrange
        User user = new User();
        user.setId("U001");
        user.setRole("ADMIN");

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userController.createUser(user)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User name is required"));
    }

    @Test
    void testCreateUser_MissingRole() {
        // Arrange
        User user = new User();
        user.setId("U001");
        user.setName("John Doe");

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userController.createUser(user)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getReason().contains("User role is required"));
    }

    @Test
    void testCreateUser_AlreadyExists() {
        // Arrange
        User user = new User();
        user.setId("U001");
        user.setName("John Doe");
        user.setRole("ADMIN");

        when(userRepository.existsById("U001")).thenReturn(true);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userController.createUser(user)
        );
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertTrue(exception.getReason().contains("already exists"));
    }

    @Test
    void testUpdateUser_Success() {
        // Arrange
        User user = new User();
        user.setId("U001");
        user.setName("John Doe Updated");
        user.setRole("PRICING_MANAGER");

        when(userRepository.existsById("U001")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenReturn(user);

        // Act
        User result = userController.updateUser("U001", user);

        // Assert
        assertNotNull(result);
        assertEquals("U001", result.getId());
        assertEquals("John Doe Updated", result.getName());
        verify(userRepository, times(1)).save(user);
    }

    @Test
    void testUpdateUser_NotFound() {
        // Arrange
        User user = new User();
        user.setName("John Doe");
        user.setRole("ADMIN");

        when(userRepository.existsById("INVALID")).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userController.updateUser("INVALID", user)
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testDeleteUser_Success() {
        // Arrange
        when(userRepository.existsById("U001")).thenReturn(true);
        doNothing().when(userRepository).deleteById("U001");

        // Act
        userController.deleteUser("U001");

        // Assert
        verify(userRepository, times(1)).deleteById("U001");
    }

    @Test
    void testDeleteUser_NotFound() {
        // Arrange
        when(userRepository.existsById("INVALID")).thenReturn(false);

        // Act & Assert
        ResponseStatusException exception = assertThrows(
            ResponseStatusException.class,
            () -> userController.deleteUser("INVALID")
        );
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void testGetUsersByRole() {
        // Arrange
        User user1 = new User();
        user1.setId("U001");
        user1.setRole("REGIONAL_MANAGER");

        User user2 = new User();
        user2.setId("U002");
        user2.setRole("REGIONAL_MANAGER");

        when(userRepository.findByRole("REGIONAL_MANAGER")).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<User> users = userController.getUsersByRole("REGIONAL_MANAGER");

        // Assert
        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> "REGIONAL_MANAGER".equals(u.getRole())));
    }

    @Test
    void testGetUsersByRegion() {
        // Arrange
        User user1 = new User();
        user1.setId("U001");
        user1.setRegion("EMEA");

        User user2 = new User();
        user2.setId("U002");
        user2.setRegion("EMEA");

        when(userRepository.findByRegion("EMEA")).thenReturn(Arrays.asList(user1, user2));

        // Act
        List<User> users = userController.getUsersByRegion("EMEA");

        // Assert
        assertEquals(2, users.size());
        assertTrue(users.stream().allMatch(u -> "EMEA".equals(u.getRegion())));
    }
}
