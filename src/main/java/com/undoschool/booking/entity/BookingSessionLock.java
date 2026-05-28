package com.undoschool.booking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "booking_session_lock")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSessionLock {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "parent_id", nullable = false)
    private Parent parent;

    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;
}
