package com.undoschool.booking.repository;

import com.undoschool.booking.entity.Booking;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, UUID> {
    boolean existsByParentIdAndOfferingId(UUID parentId, UUID offeringId);

    @EntityGraph(attributePaths = {
        "parent",
        "offering",
        "offering.course",
        "offering.teacher",
        "offering.sessions"
    })
    List<Booking> findByParentId(UUID parentId);
}
