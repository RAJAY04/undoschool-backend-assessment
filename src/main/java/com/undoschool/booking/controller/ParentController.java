package com.undoschool.booking.controller;

import com.undoschool.booking.api.ParentApi;
import com.undoschool.booking.dto.request.BookingRequest;
import com.undoschool.booking.dto.request.CreateParentRequest;
import com.undoschool.booking.dto.response.BookingResponse;
import com.undoschool.booking.dto.response.OfferingResponse;
import com.undoschool.booking.dto.response.ParentResponse;
import com.undoschool.booking.service.BookingService;
import com.undoschool.booking.service.OfferingService;
import com.undoschool.booking.service.ParentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ParentController implements ParentApi {

    private final ParentService parentService;
    private final OfferingService offeringService;
    private final BookingService bookingService;

    @Override
    public ResponseEntity<ParentResponse> createParent(CreateParentRequest request) {
        ParentResponse response = parentService.createParent(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<List<ParentResponse>> getAllParents() {
        List<ParentResponse> responses = parentService.getAllParents();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<List<OfferingResponse>> getAvailableOfferings(String timezone, String headerTimezone) {
        String resolvedTimezone = timezone;
        if (resolvedTimezone == null || resolvedTimezone.isBlank()) {
            resolvedTimezone = headerTimezone;
        }
        if (resolvedTimezone == null || resolvedTimezone.isBlank()) {
            resolvedTimezone = "UTC";
        }

        List<OfferingResponse> responses = offeringService.getAvailableOfferings(resolvedTimezone);
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<BookingResponse> bookOffering(String idempotencyKey, UUID parentId, BookingRequest request) {
        BookingResponse response = bookingService.bookOffering(parentId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<List<BookingResponse>> getParentBookings(UUID parentId) {
        List<BookingResponse> responses = bookingService.getParentBookings(parentId);
        return ResponseEntity.ok(responses);
    }
}
