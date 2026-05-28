package com.undoschool.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record BookingRequest(
    @NotNull(message = "Offering ID is required")
    UUID offeringId
) {}
