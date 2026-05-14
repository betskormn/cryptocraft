package com.cryptocraft.inventoryservice.controllers;

import com.cryptocraft.inventoryservice.controllers.dto.response.ItemPriceResponceDto;
import com.cryptocraft.inventoryservice.services.ItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/internal/prices", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Item Prices (Internal)", description = "Internal API for pricing service")
public class ItemPriceController {

    private final ItemService itemService;

    public ItemPriceController(ItemService itemService) {
        this.itemService = itemService;
    }

    @Operation(
            summary = "Get all item prices",
            description = "Returns base, min, max prices for all items (internal use only)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List of item prices"),
            @ApiResponse(responseCode = "500", description = "Unexpected error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<List<ItemPriceResponceDto>> getAllPrices() {
        return ResponseEntity.ok(itemService.getAllPrices());
    }
}
