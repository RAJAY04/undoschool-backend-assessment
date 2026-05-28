package com.undoschool.booking.api;

import com.undoschool.booking.dto.request.BookingRequest;
import com.undoschool.booking.dto.request.CreateParentRequest;
import com.undoschool.booking.dto.response.BookingResponse;
import com.undoschool.booking.dto.response.OfferingResponse;
import com.undoschool.booking.dto.response.ParentResponse;
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

@Tag(name = "Parents", description = "Parent registration, available offerings view, and bookings management")
public interface ParentApi {

    @Operation(
        summary = "Create a parent profile",
        description = "Register a parent with a name, unique email, and timezone."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Parent profile created successfully",
        content = @Content(schema = @Schema(implementation = ParentResponse.class))
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
        value = "/api/parents",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<ParentResponse> createParent(@Valid @RequestBody CreateParentRequest request);

    @Operation(
        summary = "Get all parent profiles",
        description = "Retrieve list of all registered parents."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of all parents",
        content = @Content(schema = @Schema(implementation = ParentResponse.class))
    )
    @GetMapping(
        value = "/api/parents",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<List<ParentResponse>> getAllParents();

    @Operation(
        summary = "Get available offerings",
        description = "View available offerings with sessions shifted into the request's timezone context. " +
                      "Uses the query parameter first, then fallback to X-Timezone header, then falls back to UTC."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of available offerings mapped to the target timezone",
        content = @Content(schema = @Schema(implementation = OfferingResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid timezone string provided",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(
        value = "/api/parents/offerings",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<List<OfferingResponse>> getAvailableOfferings(
        @Parameter(description = "Query parameter timezone (e.g. Europe/London)")
        @RequestParam(value = "timezone", required = false) String timezone,
        @Parameter(description = "Fallback header timezone (e.g. Asia/Kolkata)")
        @RequestHeader(value = "X-Timezone", required = false) String headerTimezone
    );

    @Operation(
        summary = "Book an offering",
        description = "Book an offering for a parent. Implements pessimistic capacity locks and checks for schedule conflicts with the parent's existing bookings."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Booking completed successfully",
        content = @Content(schema = @Schema(implementation = BookingResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Parent or Offering not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @ApiResponse(
        responseCode = "409",
        description = "Offering is full or schedule conflict locking detected",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(
        value = "/api/parents/{parentId}/bookings",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<BookingResponse> bookOffering(
        @Parameter(description = "ID of the parent booking the class", required = true)
        @PathVariable("parentId") UUID parentId,
        @Valid @RequestBody BookingRequest request
    );

    @Operation(
        summary = "Get bookings",
        description = "Get all successful class bookings for a parent."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of bookings",
        content = @Content(schema = @Schema(implementation = BookingResponse.class))
    )
    @ApiResponse(
        responseCode = "404",
        description = "Parent not found",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @GetMapping(
        value = "/api/parents/{parentId}/bookings",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<List<BookingResponse>> getParentBookings(
        @Parameter(description = "ID of the parent", required = true)
        @PathVariable("parentId") UUID parentId
    );
}
