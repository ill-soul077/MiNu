package com.minuStore.MiNu.service;

import com.minuStore.MiNu.TestFixtures;
import com.minuStore.MiNu.dto.UserRegistrationDto;
import com.minuStore.MiNu.model.Role;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User customer;
    private User seller;
    private User admin;

    @BeforeEach
    void setUp() {
        customer = TestFixtures.verifiedCustomer();
        seller = TestFixtures.verifiedSeller();
        admin = TestFixtures.adminUser();
    }

    // ══════════════════════════════════════════════════════════
    // registerUser
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("registerUser()")
    class RegisterUserTests {

        @Test
        @DisplayName("should register customer successfully")
        void registerCustomer_success() {
            UserRegistrationDto dto = TestFixtures.customerRegistrationDto();
            when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(dto.getEmail())).thenReturn(false);
            when(passwordEncoder.encode(dto.getPassword())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.registerUser(dto);

            assertThat(result.getUsername()).isEqualTo(dto.getUsername());
            assertThat(result.getEmail()).isEqualTo(dto.getEmail());
            assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
            assertThat(result.isVerified()).isFalse(); // new users are unverified
            assertThat(result.getPassword()).isEqualTo("hashed");
        }

        @Test
        @DisplayName("should register seller with SELLER role")
        void registerSeller_success() {
            UserRegistrationDto dto = TestFixtures.sellerRegistrationDto();
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.registerUser(dto);

            assertThat(result.getRole()).isEqualTo(Role.SELLER);
        }

        @Test
        @DisplayName("should default to CUSTOMER role when role is blank")
        void register_blankRole_defaultsToCustomer() {
            UserRegistrationDto dto = TestFixtures.customerRegistrationDto();
            dto.setRole("");
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.registerUser(dto);

            assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        }

        @Test
        @DisplayName("should not allow self-registration as ADMIN")
        void register_adminRole_defaultsToCustomer() {
            UserRegistrationDto dto = TestFixtures.customerRegistrationDto();
            dto.setRole("ADMIN");
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            User result = userService.registerUser(dto);

            // ADMIN should be silently rejected and fall back to CUSTOMER
            assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        }

        @Test
        @DisplayName("should encode password before saving")
        void register_passwordIsEncoded() {
            UserRegistrationDto dto = TestFixtures.customerRegistrationDto();
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode("password123")).thenReturn("BCrypt$hash");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            userService.registerUser(dto);

            assertThat(captor.getValue().getPassword()).isEqualTo("BCrypt$hash");
            assertThat(captor.getValue().getPassword()).doesNotContain("password123");
        }

        @Test
        @DisplayName("should throw when username already exists")
        void register_duplicateUsername_throws() {
            UserRegistrationDto dto = TestFixtures.customerRegistrationDto();
            when(userRepository.existsByUsername(dto.getUsername())).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Username already exists");

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("should throw when email already exists")
        void register_duplicateEmail_throws() {
            UserRegistrationDto dto = TestFixtures.customerRegistrationDto();
            when(userRepository.existsByUsername(dto.getUsername())).thenReturn(false);
            when(userRepository.existsByEmail(dto.getEmail())).thenReturn(true);

            assertThatThrownBy(() -> userService.registerUser(dto))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Email already exists");
        }

        @Test
        @DisplayName("new user should always start as unverified")
        void register_newUser_isUnverified() {
            UserRegistrationDto dto = TestFixtures.customerRegistrationDto();
            when(userRepository.existsByUsername(anyString())).thenReturn(false);
            when(userRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("hashed");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            when(userRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            userService.registerUser(dto);

            assertThat(captor.getValue().isVerified()).isFalse();
        }
    }

    // ══════════════════════════════════════════════════════════
    // verifyUser / rejectUser / deleteUser
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Admin User Actions")
    class AdminActionTests {

        @Test
        @DisplayName("verifyUser sets verified=true")
        void verifyUser_setsVerifiedTrue() {
            User unverified = TestFixtures.unverifiedCustomer();
            when(userRepository.findById(unverified.getId())).thenReturn(Optional.of(unverified));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            userService.verifyUser(unverified.getId());

            assertThat(unverified.isVerified()).isTrue();
            verify(userRepository).save(unverified);
        }

        @Test
        @DisplayName("verifyUser throws when user not found")
        void verifyUser_notFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.verifyUser(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }

        @Test
        @DisplayName("rejectUser deletes unverified user")
        void rejectUser_deletesUser() {
            User unverified = TestFixtures.unverifiedCustomer();
            when(userRepository.findById(unverified.getId())).thenReturn(Optional.of(unverified));

            userService.rejectUser(unverified.getId());

            verify(userRepository).delete(unverified);
        }

        @Test
        @DisplayName("rejectUser throws when trying to reject an admin")
        void rejectUser_admin_throws() {
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> userService.rejectUser(admin.getId()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot reject admin");

            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteUser removes a non-admin user")
        void deleteUser_success() {
            when(userRepository.findById(customer.getId())).thenReturn(Optional.of(customer));

            userService.deleteUser(customer.getId());

            verify(userRepository).delete(customer);
        }

        @Test
        @DisplayName("deleteUser throws when trying to delete an admin")
        void deleteUser_admin_throws() {
            when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

            assertThatThrownBy(() -> userService.deleteUser(admin.getId()))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Cannot delete admin");

            verify(userRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteUser throws when user not found")
        void deleteUser_notFound_throws() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.deleteUser(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("User not found");
        }
    }

    // ══════════════════════════════════════════════════════════
    // countAll / countUnverified
    // ══════════════════════════════════════════════════════════
    @Nested
    @DisplayName("Count Methods")
    class CountTests {

        @Test
        @DisplayName("countAll delegates to repository.count()")
        void countAll() {
            when(userRepository.count()).thenReturn(5L);
            assertThat(userService.countAll()).isEqualTo(5L);
        }

        @Test
        @DisplayName("countUnverified delegates to repository")
        void countUnverified() {
            when(userRepository.countByVerifiedFalse()).thenReturn(2L);
            assertThat(userService.countUnverified()).isEqualTo(2L);
        }
    }
}