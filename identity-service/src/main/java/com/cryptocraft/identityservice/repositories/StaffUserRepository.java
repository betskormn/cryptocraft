package com.cryptocraft.identityservice.repositories;

import com.cryptocraft.identityservice.dao.StaffUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StaffUserRepository extends JpaRepository<StaffUser, UUID> {

    Optional<StaffUser> findByEmailIgnoreCase(String email);

    // with venues, for tokens and the profile
    @EntityGraph(attributePaths = "memberships")
    Optional<StaffUser> findWithMembershipsByUserId(UUID userId);

    boolean existsByPlatformAdminTrue();
}
