package com.undoschool.booking.dto.request;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record SessionRequest(
    @NotNull(message = "Start time is required")
    Instant startTime,

    @NotNull(message = "End time is required")
    Instant endTime
) {}
