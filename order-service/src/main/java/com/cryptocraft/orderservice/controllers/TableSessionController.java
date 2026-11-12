package com.cryptocraft.orderservice.controllers;

import com.cryptocraft.orderservice.controllers.dto.request.JoinTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.JoinTableResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.SessionBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.TableSessionResponseDto;
import com.cryptocraft.orderservice.services.TableSessionService;
import com.cryptocraft.orderservice.controllers.dto.response.ParticipantBillResponseDto;
import com.cryptocraft.orderservice.services.GuestApprovalService;
import com.cryptocraft.orderservice.services.GuestJoinService;
import com.cryptocraft.security.VenueAccess;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/table-sessions", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Table sessions", description = "Visits at tables: guests join by QR code, staff see bills and close visits")
public class TableSessionController {

    private final TableSessionService tableSessionService;
    private final GuestJoinService guestJoinService;
    private final GuestApprovalService guestApprovalService;

    public TableSessionController(TableSessionService tableSessionService,
                                  GuestJoinService guestJoinService,
                                  GuestApprovalService guestApprovalService) {
        this.tableSessionService = tableSessionService;
        this.guestJoinService = guestJoinService;
        this.guestApprovalService = guestApprovalService;
    }

    @Operation(summary = "Join a table",
            description = "Called after scanning a table's QR code: joins the table's open visit or opens one, "
                    + "and returns the guest's token (Authorization: Bearer) for ordering and the bill. "
                    + "Sent with the guest's current token, the same guest is returned with a new token. "
                    + "A new guest's orders wait until a waiter confirms them.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Joined"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No table with this code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Table is not in service",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "503", description = "Guest sign-in is not available right now",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/join", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public JoinTableResponseDto join(@RequestBody @Valid JoinTableRequestDto requestDto,
                                     @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        UUID currentGuest = jwt == null ? null : VenueAccess.guestParticipant(jwt).orElse(null);
        return guestJoinService.join(requestDto, currentGuest);
    }

    @Operation(summary = "Confirm a guest", description = "The waiter saw the guest at the table: their held orders go to the kitchen/bar")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest confirmed; their bill"),
            @ApiResponse(responseCode = "404", description = "Guest not found at this visit",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The guest was already rejected",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{sessionId}/participants/{participantId}/approve")
    public ResponseEntity<ParticipantBillResponseDto> approveGuest(
            @Parameter(description = "Session id") @PathVariable UUID sessionId,
            @Parameter(description = "Guest id") @PathVariable UUID participantId
    ) {
        return ResponseEntity.ok(guestApprovalService.approve(sessionId, participantId));
    }

    @Operation(summary = "Reject a guest", description = "Not at the table: their held orders are cancelled and they cannot order")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Guest rejected; their bill"),
            @ApiResponse(responseCode = "404", description = "Guest not found at this visit",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "The guest is already confirmed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{sessionId}/participants/{participantId}/reject")
    public ResponseEntity<ParticipantBillResponseDto> rejectGuest(
            @Parameter(description = "Session id") @PathVariable UUID sessionId,
            @Parameter(description = "Guest id") @PathVariable UUID participantId
    ) {
        return ResponseEntity.ok(guestApprovalService.reject(sessionId, participantId));
    }

    @Operation(summary = "Get open sessions", description = "Tables with a visit in progress")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Open sessions, oldest first")
    })
    @GetMapping
    public ResponseEntity<List<TableSessionResponseDto>> getOpenSessions() {
        return ResponseEntity.ok(tableSessionService.getOpenSessions());
    }

    @Operation(summary = "Get the bill of a session", description = "All orders of the visit, split per guest")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bill"),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/{sessionId}/bill")
    public ResponseEntity<SessionBillResponseDto> getSessionBill(
            @Parameter(description = "Session id")
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(tableSessionService.getSessionBill(sessionId));
    }

    @Operation(summary = "Close a session", description = "Ends the visit after the bill is settled; no more orders can be added")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session closed"),
            @ApiResponse(responseCode = "404", description = "Session not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Session is already closed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping("/{sessionId}/close")
    public ResponseEntity<TableSessionResponseDto> closeSession(
            @Parameter(description = "Session id")
            @PathVariable UUID sessionId
    ) {
        return ResponseEntity.ok(tableSessionService.closeSession(sessionId));
    }
}
