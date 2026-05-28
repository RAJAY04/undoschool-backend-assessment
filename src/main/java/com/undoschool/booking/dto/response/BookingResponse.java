package com.undoschool.booking.dto.response;

import java.time.Instant;
import java.util.UUID;

public record BookingResponse(
    UUID bookingId,
    UUID parentId,
    UUID offeringId,
    String courseTitle,
    Instant bookedAt
) {}
