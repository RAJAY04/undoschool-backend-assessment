package com.undoschool.booking.controller;

import com.undoschool.booking.api.TeacherApi;
import com.undoschool.booking.dto.request.AddSessionsRequest;
import com.undoschool.booking.dto.request.CreateOfferingRequest;
import com.undoschool.booking.dto.request.CreateTeacherRequest;
import com.undoschool.booking.dto.response.OfferingResponse;
import com.undoschool.booking.dto.response.TeacherResponse;
import com.undoschool.booking.service.OfferingService;
import com.undoschool.booking.service.TeacherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class TeacherController implements TeacherApi {

    private final TeacherService teacherService;
    private final OfferingService offeringService;

    @Override
    public ResponseEntity<TeacherResponse> createTeacher(CreateTeacherRequest request) {
        TeacherResponse response = teacherService.createTeacher(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<List<TeacherResponse>> getAllTeachers() {
        List<TeacherResponse> responses = teacherService.getAllTeachers();
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<OfferingResponse> createOffering(UUID teacherId, CreateOfferingRequest request) {
        OfferingResponse response = offeringService.createOffering(teacherId, request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<List<OfferingResponse>> getTeacherOfferings(UUID teacherId) {
        List<OfferingResponse> responses = offeringService.getTeacherOfferings(teacherId);
        return ResponseEntity.ok(responses);
    }

    @Override
    public ResponseEntity<OfferingResponse> addSessions(String idempotencyKey, UUID offeringId, AddSessionsRequest request) {
        OfferingResponse response = offeringService.addSessions(offeringId, request);
        return ResponseEntity.ok(response);
    }
}
