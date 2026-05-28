package com.undoschool.booking.service;

import com.undoschool.booking.dto.request.BookingRequest;
import com.undoschool.booking.dto.response.BookingResponse;
import com.undoschool.booking.entity.Booking;
import com.undoschool.booking.entity.BookingSessionLock;
import com.undoschool.booking.entity.Offering;
import com.undoschool.booking.entity.Parent;
import com.undoschool.booking.entity.Session;
import com.undoschool.booking.exception.OfferingFullException;
import com.undoschool.booking.exception.ResourceNotFoundException;
import com.undoschool.booking.exception.ScheduleConflictException;
import com.undoschool.booking.mapper.BookingMapper;
import com.undoschool.booking.repository.BookingRepository;
import com.undoschool.booking.repository.BookingSessionLockRepository;
import com.undoschool.booking.repository.OfferingRepository;
import com.undoschool.booking.repository.ParentRepository;
import com.undoschool.booking.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSessionLockRepository bookingSessionLockRepository;
    private final ParentRepository parentRepository;
    private final OfferingRepository offeringRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public BookingResponse bookOffering(UUID parentId, BookingRequest request) {
        Parent parent = parentRepository.findByIdForUpdate(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found with ID: " + parentId));

        Offering offering = offeringRepository.findByIdForUpdate(request.offeringId())
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with ID: " + request.offeringId()));

        // Guard: parent hasn't already booked this exact offering
        if (bookingRepository.existsByParentIdAndOfferingId(parentId, offering.getId())) {
            throw new ScheduleConflictException("Parent has already booked this offering");
        }

        // Guard: offering still has capacity
        if (offering.getCurrentEnrollment() >= offering.getMaxStudents()) {
            throw new OfferingFullException("Offering is full: " + offering.getId());
        }

        // Lock session rows for this offering to prevent concurrent over-enrollment
        List<Session> sessions = sessionRepository.findByOfferingIdForUpdate(offering.getId());

        if (sessions.isEmpty()) {
            throw new ScheduleConflictException("Offering has no sessions and cannot be booked");
        }

        // Check for schedule conflicts against the parent's existing bookings
        for (Session session : sessions) {
            if (bookingSessionLockRepository.hasOverlappingLock(parentId, session.getStartTime(), session.getEndTime())) {
                throw new ScheduleConflictException(
                        "Session time conflict: parent already has a booking overlapping with " +
                        session.getStartTime() + " – " + session.getEndTime()
                );
            }
        }

        // All checks passed — create the booking
        Booking booking = BookingMapper.toEntity(UUID.randomUUID(), parent, offering);
        bookingRepository.save(booking);

        // Denormalize session times into booking_session_lock for fast future conflict checks
        List<BookingSessionLock> locks = new ArrayList<>();
        for (Session session : sessions) {
            locks.add(BookingMapper.toLockEntity(UUID.randomUUID(), booking, session, parent));
        }
        bookingSessionLockRepository.saveAll(locks);

        // Increment enrollment directly on the managed entity — no extra query needed
        offering.setCurrentEnrollment(offering.getCurrentEnrollment() + 1);

        return BookingMapper.toResponse(booking, ZoneId.of(parent.getTimezone()));
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getParentBookings(UUID parentId) {
        Parent parent = parentRepository.findById(parentId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent not found with ID: " + parentId));
        ZoneId parentZoneId = ZoneId.of(parent.getTimezone());

        return bookingRepository.findByParentId(parentId)
                .stream()
                .map(booking -> BookingMapper.toResponse(booking, parentZoneId))
                .toList();
    }
}
