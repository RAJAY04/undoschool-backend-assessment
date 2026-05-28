package com.undoschool.booking.repository;

import com.undoschool.booking.entity.Parent;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Parent p WHERE p.id = :parentId")
    Optional<Parent> findByIdForUpdate(@Param("parentId") UUID parentId);
}
