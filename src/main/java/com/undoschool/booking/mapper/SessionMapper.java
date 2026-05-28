package com.undoschool.booking.mapper;

import com.undoschool.booking.dto.request.SessionRequest;
import com.undoschool.booking.dto.response.SessionResponse;
import com.undoschool.booking.entity.Offering;
import com.undoschool.booking.entity.Session;

import java.time.ZoneId;
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
        return new SessionResponse(
                session.getId(),
                session.getOffering().getId(),
                session.getOffering().getTeacher().getId(),
                session.getStartTime().atZone(clientZoneId),
                session.getEndTime().atZone(clientZoneId)
        );
    }
}
