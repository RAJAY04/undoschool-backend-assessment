package com.undoschool.booking.dto.response;

import java.util.UUID;

public record TeacherResponse(
    UUID id,
    String name,
    String email,
    String timezone
) {}
