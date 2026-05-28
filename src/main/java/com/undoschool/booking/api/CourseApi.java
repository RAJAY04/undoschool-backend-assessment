package com.undoschool.booking.api;

import com.undoschool.booking.dto.request.CreateCourseRequest;
import com.undoschool.booking.dto.response.CourseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Tag(name = "Courses", description = "Course templates management")
public interface CourseApi {

    @Operation(
        summary = "Create a course template",
        description = "Create a static course template with a title and optional description."
    )
    @ApiResponse(
        responseCode = "201",
        description = "Course template created successfully",
        content = @Content(schema = @Schema(implementation = CourseResponse.class))
    )
    @ApiResponse(
        responseCode = "400",
        description = "Invalid request payload",
        content = @Content(schema = @Schema(implementation = ProblemDetail.class))
    )
    @PostMapping(
        value = "/api/courses",
        consumes = MediaType.APPLICATION_JSON_VALUE,
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<CourseResponse> createCourse(@Valid @RequestBody CreateCourseRequest request);

    @Operation(
        summary = "Get all course templates",
        description = "Retrieve list of all courses available in the system."
    )
    @ApiResponse(
        responseCode = "200",
        description = "List of all courses",
        content = @Content(schema = @Schema(implementation = CourseResponse.class))
    )
    @GetMapping(
        value = "/api/courses",
        produces = MediaType.APPLICATION_JSON_VALUE
    )
    ResponseEntity<List<CourseResponse>> getAllCourses();
}
