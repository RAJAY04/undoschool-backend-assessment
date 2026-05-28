package com.undoschool.booking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record AddSessionsRequest(
    @NotEmpty(message = "Sessions list cannot be empty")
    @Valid
    List<SessionRequest> sessions
) {}
