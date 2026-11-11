package com.cryptocraft.orderservice.controllers;

import com.cryptocraft.orderservice.controllers.dto.request.JoinTableRequestDto;
import com.cryptocraft.orderservice.controllers.dto.response.JoinTableResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.SessionBillResponseDto;
import com.cryptocraft.orderservice.controllers.dto.response.TableSessionResponseDto;
import com.cryptocraft.orderservice.services.TableSessionService;
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

    public TableSessionController(TableSessionService tableSessionService) {
        this.tableSessionService = tableSessionService;
    }

    @Operation(summary = "Join a table",
            description = "Called after scanning a table's QR code: joins the table's open visit or opens one, "
                    + "and returns the guest's participantId to send as X-Participant-Id")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Joined"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No table with this code",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Table is not in service",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(value = "/join", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public JoinTableResponseDto join(@RequestBody @Valid JoinTableRequestDto requestDto) {
        return tableSessionService.join(requestDto);
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
