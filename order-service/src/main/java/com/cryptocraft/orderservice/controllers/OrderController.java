package com.cryptocraft.orderservice.controllers;

import com.cryptocraft.orderservice.controllers.dto.request.PlaceOrderRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.OrderResponseDto;
import com.cryptocraft.orderservice.services.OrderService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
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

import java.net.URI;
import java.util.UUID;


@RestController
@RequestMapping(value = "/api/v1/orders", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Orders", description = "Guests send their cart to the kitchen/bar; prices are locked at that moment")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "Place an order",
            description = "Sends the guest's cart to the kitchen/bar. Current prices are locked and the order is "
                    + "added to the guest's bill. Idempotent by clientOrderId: a resend returns the first order with 200.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order placed"),
            @ApiResponse(responseCode = "200", description = "Already placed with this clientOrderId"),
            @ApiResponse(responseCode = "400", description = "Validation error or limits exceeded",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Participant or item not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Item on the stop-list, no current price or visit closed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Menu or prices are not available right now",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<OrderResponseDto> placeOrder(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid PlaceOrderRequestDto requestDto
    ) {
        OrderService.Result result = orderService.placeOrder(ParticipantController.guestOf(jwt), requestDto);
        if (!result.created()) {
            return ResponseEntity.ok(result.order());
        }
        URI location = URI.create("/api/v1/orders/" + result.order().orderId());
        return ResponseEntity.created(location).body(result.order());
    }

    @Operation(summary = "Get order by id")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponseDto> getOrderById(
            @Parameter(description = "Order id")
            @PathVariable UUID orderId
    ) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }
}
