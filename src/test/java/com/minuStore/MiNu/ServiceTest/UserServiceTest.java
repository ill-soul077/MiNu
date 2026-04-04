package com.minuStore.MiNu.ServiceTest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.minuStore.MiNu.dto.UserRegistrationDto;
import com.minuStore.MiNu.model.Role;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.UserRepository;
import com.minuStore.MiNu.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.crypto.password.PasswordEncoder;

public class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_success() {

        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("hassan");
        dto.setEmail("hassan@test.com");
        dto.setPassword("123456");
        dto.setRole("CUSTOMER");

        when(userRepository.existsByUsername("hassan")).thenReturn(false);
        when(userRepository.existsByEmail("hassan@test.com")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encodedPassword");

        User savedUser = User.builder()
                .username("hassan")
                .email("hassan@test.com")
                .password("encodedPassword")
                .role(Role.CUSTOMER)
                .verified(false)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(dto);

        assertNotNull(result);
        assertEquals("hassan", result.getUsername());
        assertEquals(Role.CUSTOMER, result.getRole());

        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerUser_usernameAlreadyExists() {

        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("hassan");
        dto.setEmail("hassan@test.com");

        when(userRepository.existsByUsername("hassan")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.registerUser(dto)
        );

        assertEquals("Username already exists", exception.getMessage());
    }

    @Test
    void registerUser_emailAlreadyExists() {

        UserRegistrationDto dto = new UserRegistrationDto();
        dto.setUsername("hassan");
        dto.setEmail("hassan@test.com");

        when(userRepository.existsByUsername("hassan")).thenReturn(false);
        when(userRepository.existsByEmail("hassan@test.com")).thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> userService.registerUser(dto)
        );

        assertEquals("Email already exists", exception.getMessage());
    }
}