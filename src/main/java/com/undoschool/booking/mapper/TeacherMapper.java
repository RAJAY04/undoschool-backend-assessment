package com.undoschool.booking.mapper;

import com.undoschool.booking.dto.request.CreateTeacherRequest;
import com.undoschool.booking.dto.response.TeacherResponse;
import com.undoschool.booking.entity.Teacher;

import java.util.UUID;

public final class TeacherMapper {

    private TeacherMapper() {
        // Utility class
    }

    public static Teacher toEntity(UUID id, CreateTeacherRequest request) {
        return Teacher.builder()
                .id(id)
                .name(request.name())
                .email(request.email())
                .timezone(request.timezone())
                .build();
    }

    public static TeacherResponse toResponse(Teacher teacher) {
        if (teacher == null) {
            return null;
        }
        return new TeacherResponse(
                teacher.getId(),
                teacher.getName(),
                teacher.getEmail(),
                teacher.getTimezone()
        );
    }
}
