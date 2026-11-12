package com.cryptocraft.identityservice.security;

import com.cryptocraft.identityservice.repositories.StaffUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Sign-in by email. The signed-in name is the user id, so tokens keep the same subject when the email changes.
 * Venues and roles are not authorities here: they go into the token (TokenClaimsCustomizer).
 */
@Service
@RequiredArgsConstructor
public class StaffUserDetailsService implements UserDetailsService {

    private final StaffUserRepository staffUserRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) {
        return staffUserRepository.findByEmailIgnoreCase(email.trim())
                .map(user -> User.withUsername(user.getUserId().toString())
                        .password(user.getPasswordHash())
                        .disabled(!user.isEnabled())
                        .authorities("ROLE_USER")
                        .build())
                .orElseThrow(() -> new UsernameNotFoundException("No user with this email"));
    }
}
