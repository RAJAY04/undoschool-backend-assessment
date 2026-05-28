package com.undoschool.booking.service;

import com.undoschool.booking.dto.request.CreateCourseRequest;
import com.undoschool.booking.dto.response.CourseResponse;
import com.undoschool.booking.entity.Course;
import com.undoschool.booking.mapper.CourseMapper;
import com.undoschool.booking.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    @Transactional
    public CourseResponse createCourse(CreateCourseRequest request) {
        Course course = CourseMapper.toEntity(UUID.randomUUID(), request);
        Course saved = courseRepository.save(course);
        return CourseMapper.toResponse(saved);
    }
}
