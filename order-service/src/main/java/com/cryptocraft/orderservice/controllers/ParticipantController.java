package com.cryptocraft.orderservice.controllers;

import com.cryptocraft.orderservice.controllers.dto.response.ParticipantBillResponseDto;
import com.cryptocraft.orderservice.services.TableSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping(value = "/api/v1/participants", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Participants", description = "The guest's own view of the visit")
public class ParticipantController {

    public static final String PARTICIPANT_HEADER = "X-Participant-Id";

    private final TableSessionService tableSessionService;

    public ParticipantController(TableSessionService tableSessionService) {
        this.tableSessionService = tableSessionService;
    }

    @Operation(summary = "Get my bill", description = "Everything this guest ordered during the visit, at locked prices")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bill"),
            @ApiResponse(responseCode = "400", description = "X-Participant-Id missing or malformed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Participant not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping("/me/bill")
    public ResponseEntity<ParticipantBillResponseDto> getMyBill(
            @Parameter(description = "participantId returned when joining the table")
            @RequestHeader(PARTICIPANT_HEADER) UUID participantId
    ) {
        return ResponseEntity.ok(tableSessionService.getParticipantBill(participantId));
    }
}
