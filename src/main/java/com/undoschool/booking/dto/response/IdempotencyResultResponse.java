package com.undoschool.booking.dto.response;

import com.undoschool.booking.enums.IdempotencyStatus;

public record IdempotencyResultResponse(
    IdempotencyStatus state,
    Integer responseStatus,
    String responseBody
) {}
