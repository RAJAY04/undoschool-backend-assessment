package com.undoschool.booking.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.undoschool.booking.dto.response.IdempotencyResultResponse;
import com.undoschool.booking.entity.IdempotentRequest;
import com.undoschool.booking.enums.IdempotencyStatus;
import com.undoschool.booking.repository.IdempotentRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotentRequestRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public IdempotencyResultResponse startRequest(String key) {
        if (key == null || key.isBlank()) {
            return new IdempotencyResultResponse(IdempotencyStatus.CREATED, null, null);
        }

        Optional<IdempotentRequest> existingOpt = repository.findById(key);
        if (existingOpt.isPresent()) {
            IdempotentRequest existing = existingOpt.get();
            if (IdempotencyStatus.PENDING == existing.getStatus()) {
                return new IdempotencyResultResponse(IdempotencyStatus.PENDING, null, null);
            }
            return new IdempotencyResultResponse(IdempotencyStatus.SUCCESS, existing.getResponseStatus(), existing.getResponseBody());
        }

        try {
            IdempotentRequest pendingRequest = IdempotentRequest.builder()
                    .key(key)
                    .status(IdempotencyStatus.PENDING)
                    .createdAt(Instant.now())
                    .build();
            repository.saveAndFlush(pendingRequest);
            return new IdempotencyResultResponse(IdempotencyStatus.CREATED, null, null);
        } catch (Exception e) {
            // Concurrent insert of the same key results in a unique key violation.
            // Treat as pending request in progress.
            return new IdempotencyResultResponse(IdempotencyStatus.PENDING, null, null);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeRequest(String key, int status, Object responseBodyObj) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(responseBodyObj);
            repository.findById(key).ifPresent(req -> {
                req.setStatus(IdempotencyStatus.SUCCESS);
                req.setResponseStatus(status);
                req.setResponseBody(json);
                repository.saveAndFlush(req);
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize response body for idempotency caching", e);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failRequest(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        repository.deleteById(key);
        repository.flush();
    }
}
