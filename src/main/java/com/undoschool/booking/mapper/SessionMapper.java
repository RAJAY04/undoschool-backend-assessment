package com.undoschool.booking.mapper;

import com.undoschool.booking.dto.request.SessionRequest;
import com.undoschool.booking.dto.response.SessionResponse;
import com.undoschool.booking.entity.Offering;
import com.undoschool.booking.entity.Session;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class SessionMapper {

    private SessionMapper() {
        // Utility class
    }

    public static Session toEntity(UUID id, Offering offering, SessionRequest request) {
        return Session.builder()
                .id(id)
                .offering(offering)
                .startTime(request.startTime())
                .endTime(request.endTime())
                .build();
    }

    public static SessionResponse toResponse(Session session, ZoneId clientZoneId) {
        if (session == null) {
            return null;
        }
        ZonedDateTime start = session.getStartTime().atZone(clientZoneId);
        ZonedDateTime end = session.getEndTime().atZone(clientZoneId);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a (z)");

        return new SessionResponse(
                session.getId(),
                session.getOffering().getId(),
                session.getOffering().getTeacher().getId(),
                start,
                end,
                start.format(formatter),
                end.format(formatter)
        );
    }
}
