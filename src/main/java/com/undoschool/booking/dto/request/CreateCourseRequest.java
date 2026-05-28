package com.undoschool.booking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CreateCourseRequest(
    @NotBlank(message = "Title is required")
    String title,

    String description
) {}
