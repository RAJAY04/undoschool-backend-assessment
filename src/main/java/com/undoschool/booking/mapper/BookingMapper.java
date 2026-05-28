package com.undoschool.booking.mapper;

import com.undoschool.booking.dto.response.BookingResponse;
import com.undoschool.booking.dto.response.SessionResponse;
import com.undoschool.booking.entity.Booking;
import com.undoschool.booking.entity.BookingSessionLock;
import com.undoschool.booking.entity.Offering;
import com.undoschool.booking.entity.Parent;
import com.undoschool.booking.entity.Session;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public final class BookingMapper {

    private BookingMapper() {
        // Utility class
    }

    public static BookingResponse toResponse(Booking booking, ZoneId clientZoneId) {
        if (booking == null) {
            return null;
        }

        List<SessionResponse> sessions = booking.getOffering().getSessions()
                .stream()
                .map(session -> SessionMapper.toResponse(session, clientZoneId))
                .toList();

        return new BookingResponse(
                booking.getId(),
                booking.getParent().getId(),
                booking.getOffering().getId(),
                booking.getOffering().getCourse().getTitle(),
                booking.getBookedAt(),
                sessions
        );
    }

    public static Booking toEntity(UUID id, Parent parent, Offering offering) {
        return Booking.builder()
                .id(id)
                .parent(parent)
                .offering(offering)
                .build();
    }

    public static BookingSessionLock toLockEntity(UUID id, Booking booking, Session session, Parent parent) {
        return BookingSessionLock.builder()
                .id(id)
                .booking(booking)
                .session(session)
                .parent(parent)
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .build();
    }
}
