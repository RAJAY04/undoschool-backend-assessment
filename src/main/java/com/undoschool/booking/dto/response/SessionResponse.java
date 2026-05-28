package com.undoschool.booking.dto.response;

import java.time.ZonedDateTime;
import java.util.UUID;

public record SessionResponse(
    UUID sessionId,
    UUID offeringId,
    UUID teacherId,
    ZonedDateTime startTime,
    ZonedDateTime endTime
) {}
