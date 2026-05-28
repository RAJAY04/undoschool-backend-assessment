package com.undoschool.booking.service;

import com.undoschool.booking.dto.request.CreateTeacherRequest;
import com.undoschool.booking.dto.response.TeacherResponse;
import com.undoschool.booking.entity.Teacher;
import com.undoschool.booking.mapper.TeacherMapper;
import com.undoschool.booking.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;

    @Transactional
    public TeacherResponse createTeacher(CreateTeacherRequest request) {
        // Automatically throws DateTimeException (mapped to 400 Bad Request) if invalid
        ZoneId.of(request.timezone());

        Teacher teacher = TeacherMapper.toEntity(UUID.randomUUID(), request);
        Teacher saved = teacherRepository.save(teacher);
        return TeacherMapper.toResponse(saved);
    }
}
