package com.undoschool.booking.dto.response;

import java.util.List;
import java.util.UUID;

public record OfferingResponse(
    UUID offeringId,
    String courseTitle,
    String teacherName,
    int maxStudents,
    int currentEnrollment,
    List<SessionResponse> sessions
) {}
