package com.cryptocraft.identityservice.controllers;

import com.cryptocraft.identityservice.controllers.dto.request.PasswordChangeRequestDto;
import com.cryptocraft.identityservice.controllers.dto.response.MeResponseDto;
import com.cryptocraft.identityservice.services.AccountService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/me", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Me", description = "The signed-in person's account")
public class MeController {

    private final AccountService accountService;

    public MeController(AccountService accountService) {
        this.accountService = accountService;
    }

    @Operation(summary = "Get my account", description = "Name, platform admin flag and the venues with my role in each")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account"),
            @ApiResponse(responseCode = "401", description = "Not signed in",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Account removed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping
    public ResponseEntity<MeResponseDto> getMe(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(accountService.getMe(userId(jwt)));
    }

    @Operation(summary = "Change my password")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The current password is wrong",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal Jwt jwt,
                                               @RequestBody @Valid PasswordChangeRequestDto requestDto) {
        accountService.changePassword(userId(jwt), requestDto);
        return ResponseEntity.noContent().build();
    }

    // a person's token has their user id as subject; a service token does not
    private static UUID userId(Jwt jwt) {
        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("This token does not belong to a person");
        }
    }
}
