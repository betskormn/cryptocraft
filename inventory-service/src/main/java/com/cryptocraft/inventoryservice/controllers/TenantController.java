package com.cryptocraft.inventoryservice.controllers;

import com.cryptocraft.inventoryservice.controllers.dto.request.TenantRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.request.TenantUpdateRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.TenantResponseDto;
import com.cryptocraft.inventoryservice.services.TenantService;
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
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

// TODO: restrict to platform admins once the gateway validates JWTs; not tenant-scoped, see TenantFilter
@RestController
@RequestMapping(value = "/api/v1/platform/tenants", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tenants (Platform)", description = "Platform API for managing venues")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @Operation(summary = "Create tenant",
            description = "Creates a venue; with demoMenu=true copies the demo template menu into it")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tenant created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Tenant slug or name already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantResponseDto> createTenant(@RequestBody @Valid TenantRequestDto requestDto) {
        TenantResponseDto created = tenantService.createTenant(requestDto);
        URI location = URI.create("/api/v1/platform/tenants/" + created.tenantId());
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Get all tenants")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of tenants")
    })
    @GetMapping
    public ResponseEntity<List<TenantResponseDto>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    @Operation(summary = "Get tenant by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant found"),
            @ApiResponse(responseCode = "404", description = "Tenant not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<TenantResponseDto> getTenantById(
            @Parameter(description = "Tenant id", example = "1")
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    @Operation(summary = "Get tenant by slug")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant found"),
            @ApiResponse(responseCode = "404", description = "Tenant not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/by-slug/{slug}")
    public ResponseEntity<TenantResponseDto> getTenantBySlug(
            @Parameter(description = "Tenant slug", example = "default")
            @PathVariable String slug
    ) {
        return ResponseEntity.ok(tenantService.getTenantBySlug(slug));
    }

    @Operation(summary = "Update tenant by id", description = "Full update (PUT) of venue settings; slug is immutable")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tenant updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tenant not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TenantResponseDto> updateTenant(
            @Parameter(description = "Tenant id", example = "1")
            @PathVariable @Positive Long id,
            @RequestBody @Valid TenantUpdateRequestDto requestDto
    ) {
        return ResponseEntity.ok(tenantService.updateTenant(id, requestDto));
    }
}
