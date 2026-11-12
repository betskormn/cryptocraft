package com.cryptocraft.identityservice.controllers;

import com.cryptocraft.identityservice.controllers.dto.request.GuestTokenRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.GuestTokenResponseDto;
import com.cryptocraft.identityservice.services.GuestTokenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * For Order Service only (client credentials, scope "internal"); not routed by the gateway.
 */
@RestController
@RequestMapping(value = "/api/v1/guest-tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Guest tokens (internal)", description = "Tokens for guests who joined a table by QR code")
public class GuestTokenController {

    private final GuestTokenService guestTokenService;

    public GuestTokenController(GuestTokenService guestTokenService) {
        this.guestTokenService = guestTokenService;
    }

    @Operation(summary = "Issue a guest token", description = "Signed like staff tokens; names the venue, the visit and the guest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not one of our services",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GuestTokenResponseDto> issue(@RequestBody @Valid GuestTokenRequestDto requestDto) {
        return ResponseEntity.ok(guestTokenService.issue(requestDto));
    }
}
