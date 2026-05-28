package com.undoschool.booking.mapper;

import com.undoschool.booking.dto.request.CreateOfferingRequest;
import com.undoschool.booking.dto.response.OfferingResponse;
import com.undoschool.booking.dto.response.SessionResponse;
import com.undoschool.booking.entity.Course;
import com.undoschool.booking.entity.Offering;
import com.undoschool.booking.entity.Teacher;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class OfferingMapper {

    private OfferingMapper() {
        // Utility class
    }

    public static Offering toEntity(UUID id, Teacher teacher, Course course, CreateOfferingRequest request) {
        return Offering.builder()
                .id(id)
                .course(course)
                .teacher(teacher)
                .title(request.title())
                .maxStudents(request.maxStudents())
                .currentEnrollment(0)
                .sessions(new ArrayList<>())
                .build();
    }

    public static OfferingResponse toResponse(Offering offering, ZoneId clientZoneId) {
        if (offering == null) {
            return null;
        }

        List<SessionResponse> sessionResponses = new ArrayList<>();
        if (offering.getSessions() != null) {
            for (var session : offering.getSessions()) {
                sessionResponses.add(SessionMapper.toResponse(session, clientZoneId));
            }
        }

        return new OfferingResponse(
                offering.getId(),
                offering.getCourse().getTitle(),
                offering.getTeacher().getName(),
                offering.getMaxStudents(),
                offering.getCurrentEnrollment(),
                sessionResponses
        );
    }
}
