package com.undoschool.booking.api;

import com.undoschool.booking.annotation.Idempotent;
import com.undoschool.booking.dto.request.AddSessionsRequest;
import com.undoschool.booking.dto.request.CreateOfferingRequest;
import com.undoschool.booking.dto.request.CreateTeacherRequest;
import com.undoschool.booking.dto.response.OfferingResponse;
import com.undoschool.booking.dto.response.TeacherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Teachers", description = "Teacher onboarding, offerings, and sessions management")
public interface TeacherApi {

    @Operation(
        summary = "Create a teacher profile",
        description = "Register a teacher with a name, unique email, and timezone."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Teacher profile created successfully",
        content = @Content(schema = @Schema(implementation = TeacherResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload or timezone",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
        responseCode = "499",
        description = "Email already exists (Conflict)",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(
        value = "/api/teachers",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<TeacherResponse> createTeacher(@Valid @RequestBody CreateTeacherRequest request);

    @Operation(
        summary = "Get all teacher profiles",
        description = "Retrieve list of all registered teachers."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of all teachers",
        content = @Content(schema = @Schema(implementation = TeacherResponse.class))
    )
    @GetMapping(
        value = "/api/teachers",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<List<TeacherResponse>> getAllTeachers();

    @Operation(
        summary = "Create a new offering/section",
        description = "Create an offering of a course by a teacher. Max students limit applies here."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Offering created successfully",
        content = @Content(schema = @Schema(implementation = OfferingResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Teacher or Course not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(
        value = "/api/teachers/{teacherId}/offerings",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<OfferingResponse> createOffering(
        @Parameter(description = "ID of the teacher offering the course", required = true)
        @PathVariable("teacherId") UUID teacherId,
        @Valid @RequestBody CreateOfferingRequest request
    );

    @Operation(
        summary = "Get teacher offerings",
        description = "Get list of all offerings and scheduled sessions belonging to a teacher."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of offerings",
        content = @Content(schema = @Schema(implementation = OfferingResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Teacher not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(
        value = "/api/teachers/{teacherId}/offerings",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<List<OfferingResponse>> getTeacherOfferings(
        @Parameter(description = "ID of the teacher", required = true)
        @PathVariable("teacherId") UUID teacherId
    );

    @Operation(
        summary = "Add sessions to an offering",
        description = "Schedule meetings/sessions for an offering. Validates bounds and checks for overlaps within the request sessions."
    )
    @ApiResponse(
        responseCode = "200",
        description = "Sessions successfully scheduled, returns offering with new sessions",
        content = @Content(schema = @Schema(implementation = OfferingResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid dates or session time overlaps in payload",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Offering not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @Idempotent
    @PostMapping(
        value = "/api/offerings/{offeringId}/sessions",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<OfferingResponse> addSessions(
        @Parameter(description = "Optional client-generated key to ensure request idempotency", required = false)
        @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
        @Parameter(description = "ID of the offering", required = true)
        @PathVariable("offeringId") UUID offeringId,
        @Valid @RequestBody AddSessionsRequest request
    );
}
