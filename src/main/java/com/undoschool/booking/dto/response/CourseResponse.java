package com.undoschool.booking.dto.response;

import java.util.UUID;

public record CourseResponse(
    UUID id,
    String title,
    String description
) {}
