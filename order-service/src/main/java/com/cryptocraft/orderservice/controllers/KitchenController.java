package com.cryptocraft.orderservice.controllers;

import com.cryptocraft.orderservice.controllers.dto.request.LineStatusRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.KitchenLineResponseDto;
import com.cryptocraft.orderservice.dao.enums.ItemType;
import com.cryptocraft.orderservice.services.KitchenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/v1/kitchen", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Kitchen", description = "Kitchen and bar screens (KDS): the queue of sent lines and their statuses")
public class KitchenController {

    private final KitchenService kitchenService;

    public KitchenController(KitchenService kitchenService) {
        this.kitchenService = kitchenService;
    }

    @Operation(summary = "Get the queue of a station",
            description = "Lines still in work (NEW, PREPARING, READY) in the order they were sent. "
                    + "Live changes come through the WebSocket /ws/kds/{tenantId}")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Queue, oldest first"),
            @ApiResponse(responseCode = "400", description = "Unknown item type",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/lines")
    public ResponseEntity<List<KitchenLineResponseDto>> getQueue(
            @Parameter(description = "MEAL for the kitchen, DRINK for the bar")
            @RequestParam ItemType itemType
    ) {
        return ResponseEntity.ok(kitchenService.getQueue(itemType));
    }

    @Operation(summary = "Change the status of a line",
            description = "NEW, PREPARING and READY can be changed in any direction; SERVED and CANCELLED are final. "
                    + "A cancelled line is taken off the guest's bill; the stock already written off is not returned")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status changed (or already had it)"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Line not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Line is already served or cancelled",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping(value = "/lines/{lineId}/status", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<KitchenLineResponseDto> changeStatus(
            @Parameter(description = "Line id")
            @PathVariable Long lineId,
            @RequestBody @Valid LineStatusRequestDto requestDto
    ) {
        return ResponseEntity.ok(kitchenService.changeStatus(lineId, requestDto));
    }
}
