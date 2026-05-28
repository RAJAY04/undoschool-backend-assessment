package com.undoschool.booking.repository;

import com.undoschool.booking.entity.BookingSessionLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

@Repository
public interface BookingSessionLockRepository extends JpaRepository<BookingSessionLock, UUID> {

    @Query("SELECT EXISTS(" +
           "  SELECT 1 FROM BookingSessionLock bsl " +
           "  WHERE bsl.parent.id = :parentId " +
           "    AND bsl.startTime < :endTime " +
           "    AND bsl.endTime > :startTime" +
           ")")
    boolean hasOverlappingLock(
        @Param("parentId") UUID parentId,
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime
    );
}
