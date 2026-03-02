package com.minuStore.MiNu.config;

import com.minuStore.MiNu.model.Role;
import com.minuStore.MiNu.model.User;
import com.minuStore.MiNu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@minu.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role(Role.ADMIN)
                    .verified(true)
                    .build();
            userRepository.save(admin);
            log.info("Default ADMIN user created: admin / admin123");
        }

        if (userRepository.findByUsername("seller").isEmpty()) {
            User seller = User.builder()
                    .username("seller")
                    .email("seller@minu.com")
                    .password(passwordEncoder.encode("seller123"))
                    .role(Role.SELLER)
                    .verified(true)
                    .build();
            userRepository.save(seller);
            log.info("Default SELLER user created: seller / seller123 (verified)");
        }

        if (userRepository.findByUsername("customer").isEmpty()) {
            User customer = User.builder()
                    .username("customer")
                    .email("customer@minu.com")
                    .password(passwordEncoder.encode("customer123"))
                    .role(Role.CUSTOMER)
                    .verified(true)
                    .build();
            userRepository.save(customer);
            log.info("Default CUSTOMER user created: customer / customer123 (verified)");
        }
    }
}
