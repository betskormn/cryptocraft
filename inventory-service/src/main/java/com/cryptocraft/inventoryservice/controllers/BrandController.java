package com.cryptocraft.inventoryservice.controllers;

import com.cryptocraft.inventoryservice.controllers.dto.request.BrandRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.BrandResponseDto;
import com.cryptocraft.inventoryservice.services.BrandService;
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

@RestController
@RequestMapping(value = "/api/v1/brands", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Brands", description = "Admin API for managing brands in inventory")
public class BrandController {

    private final BrandService brandService;

    public BrandController(BrandService brandService) {
        this.brandService = brandService;
    }

    @Operation(summary = "Create brand", description = "Creates a new brand")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Brand created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Brand already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BrandResponseDto> createBrand(@RequestBody @Valid BrandRequestDto requestDto) {
        BrandResponseDto created = brandService.createBrand(requestDto);
        URI location = URI.create("/api/v1/brands/" + created.brandId());
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Get all brands")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of brands")
    })
    @GetMapping
    public ResponseEntity<List<BrandResponseDto>> getAllBrands() {
        return ResponseEntity.ok(brandService.getAllBrands());
    }

    @Operation(summary = "Get brand by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Brand found"),
            @ApiResponse(responseCode = "404", description = "Brand not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<BrandResponseDto> getBrandById(
            @Parameter(description = "Brand id", example = "1")
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(brandService.getBrandById(id));
    }

    @Operation(summary = "Delete brand by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Brand deleted"),
            @ApiResponse(responseCode = "404", description = "Brand not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBrand(
            @Parameter(description = "Brand id", example = "1")
            @PathVariable @Positive Long id
    ) {
        brandService.deleteBrand(id);
        return ResponseEntity.noContent().build();
    }
}