package com.undoschool.booking.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateOfferingRequest(
    @NotNull(message = "Course ID is required")
    UUID courseId,

    @NotBlank(message = "Title is required")
    String title,

    @NotNull(message = "Max students is required")
    @Min(value = 1, message = "Max students must be at least 1")
    Integer maxStudents
) {}
