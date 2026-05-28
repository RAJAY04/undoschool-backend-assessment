package com.undoschool.booking.repository;

import com.undoschool.booking.entity.Offering;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OfferingRepository extends JpaRepository<Offering, UUID> {
    List<Offering> findByTeacherId(UUID teacherId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Offering o WHERE o.id = :offeringId")
    Optional<Offering> findByIdForUpdate(@Param("offeringId") UUID offeringId);
}
