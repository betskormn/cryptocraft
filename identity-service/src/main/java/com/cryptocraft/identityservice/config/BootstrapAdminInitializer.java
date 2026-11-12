package com.cryptocraft.identityservice.config;

import com.cryptocraft.identityservice.dao.StaffUser;
import com.cryptocraft.identityservice.repositories.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * The first platform admin, so a fresh installation can be signed in to. Created only while there is no
 * platform admin; its password should be changed after the first sign-in.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final IdentityProperties properties;
    private final StaffUserRepository staffUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        IdentityProperties.BootstrapAdmin admin = properties.bootstrapAdmin();
        if (admin == null || admin.email() == null || admin.email().isBlank() || staffUserRepository.existsByPlatformAdminTrue()) {
            return;
        }
        if (staffUserRepository.findByEmailIgnoreCase(admin.email()).isPresent()) {
            log.warn("Bootstrap admin {} exists but is not a platform admin; nothing changed", admin.email());
            return;
        }
        staffUserRepository.save(StaffUser.builder()
                .userId(UUID.randomUUID())
                .email(admin.email().trim())
                .displayName(admin.displayName() != null ? admin.displayName() : "Platform admin")
                .passwordHash(passwordEncoder.encode(admin.password()))
                .enabled(true)
                .platformAdmin(true)
                .build());
        log.info("Created platform admin {}", admin.email());
    }
}
