package com.undoschool.booking.repository;

import com.undoschool.booking.entity.IdempotentRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.Instant;

@Repository
public interface IdempotentRequestRepository extends JpaRepository<IdempotentRequest, String> {

    @Modifying
    void deleteByCreatedAtBefore(Instant cutoff);
}
