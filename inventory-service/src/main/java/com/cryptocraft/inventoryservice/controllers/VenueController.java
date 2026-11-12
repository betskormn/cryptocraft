package com.cryptocraft.inventoryservice.controllers;

import com.cryptocraft.inventoryservice.controllers.dto.response.TenantResponseDto;
import com.cryptocraft.inventoryservice.services.TenantService;
import com.cryptocraft.security.VenueAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * The venue switcher of a signed-in person: the venues their token lists; a platform admin sees all.
 * Not tenant-scoped (see tenant.free-paths).
 */
@RestController
@RequestMapping(value = "/api/v1/venues", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Venues", description = "Venues of the signed-in person")
public class VenueController {

    private final TenantService tenantService;

    public VenueController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(summary = "Get my venues")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Venues by name"),
            @ApiResponse(responseCode = "401", description = "Not signed in",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<TenantResponseDto>> getMyVenues(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(VenueAccess.isPlatformAdmin(jwt)
                ? tenantService.getAllTenants()
                : tenantService.getTenantsByIds(VenueAccess.venues(jwt).keySet()));
    }
}
