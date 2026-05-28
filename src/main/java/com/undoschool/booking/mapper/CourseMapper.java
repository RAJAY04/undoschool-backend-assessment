package com.undoschool.booking.mapper;

import com.undoschool.booking.dto.request.CreateCourseRequest;
import com.undoschool.booking.dto.response.CourseResponse;
import com.undoschool.booking.entity.Course;

import java.util.UUID;

public final class CourseMapper {

    private CourseMapper() {
        // Utility class
    }

    public static Course toEntity(UUID id, CreateCourseRequest request) {
        return Course.builder()
                .id(id)
                .title(request.title())
                .description(request.description())
                .build();
    }

    public static CourseResponse toResponse(Course course) {
        if (course == null) {
            return null;
        }
        return new CourseResponse(
                course.getId(),
                course.getTitle(),
                course.getDescription()
        );
    }
}
