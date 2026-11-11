package com.cryptocraft.orderservice.controllers;

import com.cryptocraft.orderservice.controllers.dto.request.VenueTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.VenueTableResponseDto;
import com.cryptocraft.orderservice.services.VenueTableService;
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
@RequestMapping(value = "/api/v1/tables", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tables", description = "Admin API for the venue's tables and their QR codes")
public class VenueTableController {

    private final VenueTableService venueTableService;

    public VenueTableController(VenueTableService venueTableService) {
        this.venueTableService = venueTableService;
    }

    @Operation(summary = "Create table", description = "Creates a table with a new QR code token")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Table created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Table with this label already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VenueTableResponseDto> createTable(@RequestBody @Valid VenueTableRequestDto requestDto) {
        VenueTableResponseDto created = venueTableService.createTable(requestDto);
        URI location = URI.create("/api/v1/tables/" + created.tableId());
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Get all tables")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tables by label")
    })
    @GetMapping
    public ResponseEntity<List<VenueTableResponseDto>> getAllTables() {
        return ResponseEntity.ok(venueTableService.getAllTables());
    }

    @Operation(summary = "Update table by id", description = "Renames the table or takes it out of service; the QR code stays")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Table updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Table not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Table with this label already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<VenueTableResponseDto> updateTable(
            @Parameter(description = "Table id", example = "1")
            @PathVariable @Positive Long id,
            @RequestBody @Valid VenueTableRequestDto requestDto
    ) {
        return ResponseEntity.ok(venueTableService.updateTable(id, requestDto));
    }
}
