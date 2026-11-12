package com.cryptocraft.identityservice.controllers;

import com.cryptocraft.identityservice.controllers.dto.request.StaffRequestDto;
import com.cryptocraft.identityservice.controllers.dto.request.StaffRoleRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.StaffResponseDto;
import com.cryptocraft.identityservice.services.StaffService;
import com.cryptocraft.security.VenueAccess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * Venue admins manage who works in their venue (X-Tenant-Id) and with what role.
 */
@RestController
@Validated
@RequestMapping(value = "/api/v1/staff", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Staff", description = "Admin API for the venue's staff and their roles")
public class StaffController {

    private final StaffService staffService;

    public StaffController(StaffService staffService) {
        this.staffService = staffService;
    }

    @Operation(summary = "Get the venue's staff")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Staff by name"),
            @ApiResponse(responseCode = "403", description = "Not an admin of this venue",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<StaffResponseDto>> getStaff(
            @Parameter(description = "Venue id", example = "1")
            @RequestHeader(VenueAccess.TENANT_HEADER) @Positive Long tenantId
    ) {
        return ResponseEntity.ok(staffService.getStaff(tenantId));
    }

    @Operation(summary = "Add a person to the venue",
            description = "A new email creates an account with the given initial password; an existing account just gets the venue")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Added"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not an admin of this venue",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Already works in this venue",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StaffResponseDto> addStaff(
            @Parameter(description = "Venue id", example = "1")
            @RequestHeader(VenueAccess.TENANT_HEADER) @Positive Long tenantId,
            @RequestBody @Valid StaffRequestDto requestDto
    ) {
        StaffResponseDto created = staffService.addStaff(tenantId, requestDto);
        return ResponseEntity.created(URI.create("/api/v1/staff/" + created.userId())).body(created);
    }

    @Operation(summary = "Change a person's role in the venue")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role changed"),
            @ApiResponse(responseCode = "404", description = "Does not work in this venue",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The venue would be left without an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/{userId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<StaffResponseDto> changeRole(
            @Parameter(description = "Venue id", example = "1")
            @RequestHeader(VenueAccess.TENANT_HEADER) @Positive Long tenantId,
            @PathVariable UUID userId,
            @RequestBody @Valid StaffRoleRequestDto requestDto
    ) {
        return ResponseEntity.ok(staffService.changeRole(tenantId, userId, requestDto));
    }

    @Operation(summary = "Remove a person from the venue", description = "The account stays for the person's other venues")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Removed"),
            @ApiResponse(responseCode = "404", description = "Does not work in this venue",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The venue would be left without an admin",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> removeStaff(
            @Parameter(description = "Venue id", example = "1")
            @RequestHeader(VenueAccess.TENANT_HEADER) @Positive Long tenantId,
            @PathVariable UUID userId
    ) {
        staffService.removeStaff(tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
