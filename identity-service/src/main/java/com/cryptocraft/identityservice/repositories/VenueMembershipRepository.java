package com.cryptocraft.identityservice.repositories;

import com.cryptocraft.identityservice.dao.VenueMembership;
import com.cryptocraft.security.VenueRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VenueMembershipRepository extends JpaRepository<VenueMembership, VenueMembership.Id> {

    @Query("""
            select m from VenueMembership m join fetch m.user u
            where m.id.tenantId = :tenantId
            order by lower(u.displayName), lower(u.email)
            """)
    List<VenueMembership> findStaffOfVenue(Long tenantId);

    long countByIdTenantIdAndRole(Long tenantId, VenueRole role);
}
