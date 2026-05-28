package com.undoschool.booking.controller;

import com.undoschool.booking.api.CourseApi;
import com.undoschool.booking.dto.request.CreateCourseRequest;
import com.undoschool.booking.dto.response.CourseResponse;
import com.undoschool.booking.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CourseController implements CourseApi {

    private final CourseService courseService;

    @Override
    public ResponseEntity<CourseResponse> createCourse(CreateCourseRequest request) {
        CourseResponse response = courseService.createCourse(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<List<CourseResponse>> getAllCourses() {
        List<CourseResponse> responses = courseService.getAllCourses();
        return ResponseEntity.ok(responses);
    }
}
