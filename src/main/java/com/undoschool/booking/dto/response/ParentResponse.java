package com.undoschool.booking.dto.response;

import java.util.UUID;

public record ParentResponse(
    UUID id,
    String name,
    String email,
    String timezone
) {}
