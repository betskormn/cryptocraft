package com.cryptocraft.inventoryservice.controllers;

import com.cryptocraft.inventoryservice.controllers.dto.request.ItemRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.request.StopListRequestDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.ItemResponseDto;
import com.cryptocraft.inventoryservice.controllers.dto.response.SellableItemResponseDto;
import com.cryptocraft.inventoryservice.services.ItemService;
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
@RequestMapping(value = "/api/v1/items", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Items", description = "Admin API for managing inventory items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(summary = "Create item", description = "Creates a new item in inventory")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Item created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Item already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemResponseDto> createItem(@RequestBody @Valid ItemRequestDto requestDto) {
        ItemResponseDto created = itemService.createItem(requestDto);
        URI location = URI.create("/api/v1/items/" + created.itemId());
        return ResponseEntity.created(location).body(created);
    }

    @Operation(summary = "Get all items", description = "Returns all items (admin view)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of items")
    })
    @GetMapping
    public ResponseEntity<List<ItemResponseDto>> getAllItems() {
        return ResponseEntity.ok(itemService.getAllItems());
    }

    @Operation(summary = "Get sellable items",
            description = "Items in menu order with stop-list state and an estimate of portions left in stock; "
                    + "the estimate is a warning only and never blocks an order. "
                    + "With itemIds only those items are returned (unknown ids are left out)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Items in menu order")
    })
    @GetMapping("/sellable")
    public ResponseEntity<List<SellableItemResponseDto>> getSellableItems(
            @Parameter(description = "Only these items, e.g. the lines of an order", example = "1,2")
            @RequestParam(required = false) List<Long> itemIds
    ) {
        return ResponseEntity.ok(itemService.getSellableItems(itemIds));
    }

    @Operation(summary = "Get item by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item found"),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ItemResponseDto> getItemById(
            @Parameter(description = "Item id", example = "1")
            @PathVariable @Positive Long id
    ) {
        return ResponseEntity.ok(itemService.getItemById(id));
    }

    @Operation(summary = "Update item by id", description = "Full update (PUT) of item data")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Item updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemResponseDto> updateItemById(
            @Parameter(description = "Item id", example = "1")
            @PathVariable @Positive Long id,
            @RequestBody @Valid ItemRequestDto requestDto
    ) {
        return ResponseEntity.ok(itemService.updateItemById(id, requestDto));
    }

    @Operation(summary = "Put item on / take item off the stop-list",
            description = "A stopped item stays on the menu but cannot be ordered")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Stop-list state changed"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping(value = "/{id}/stop-list", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ItemResponseDto> setStopped(
            @Parameter(description = "Item id", example = "1")
            @PathVariable @Positive Long id,
            @RequestBody @Valid StopListRequestDto request
    ) {
        return ResponseEntity.ok(itemService.setStopped(id, request.stopped()));
    }

    @Operation(summary = "Delete item by id")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Item deleted"),
            @ApiResponse(responseCode = "404", description = "Item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(
            @Parameter(description = "Item id", example = "1")
            @PathVariable @Positive Long id
    ) {
        itemService.deleteItemById(id);
        return ResponseEntity.noContent().build();
    }
}