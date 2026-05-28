package com.undoschool.booking.service;

import com.undoschool.booking.dto.request.AddSessionsRequest;
import com.undoschool.booking.dto.request.CreateOfferingRequest;
import com.undoschool.booking.dto.request.SessionRequest;
import com.undoschool.booking.dto.response.OfferingResponse;
import com.undoschool.booking.entity.Course;
import com.undoschool.booking.entity.Offering;
import com.undoschool.booking.entity.Session;
import com.undoschool.booking.entity.Teacher;
import com.undoschool.booking.exception.ResourceNotFoundException;
import com.undoschool.booking.mapper.OfferingMapper;
import com.undoschool.booking.mapper.SessionMapper;
import com.undoschool.booking.repository.CourseRepository;
import com.undoschool.booking.repository.OfferingRepository;
import com.undoschool.booking.repository.SessionRepository;
import com.undoschool.booking.repository.TeacherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferingService {

    private final OfferingRepository offeringRepository;
    private final CourseRepository courseRepository;
    private final TeacherRepository teacherRepository;
    private final SessionRepository sessionRepository;

    @Transactional
    public OfferingResponse createOffering(UUID teacherId, CreateOfferingRequest request) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + teacherId));
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with ID: " + request.courseId()));

        Offering offering = OfferingMapper.toEntity(UUID.randomUUID(), teacher, course, request);

        Offering saved = offeringRepository.save(offering);
        ZoneId teacherZoneId = ZoneId.of(teacher.getTimezone());

        return OfferingMapper.toResponse(saved, teacherZoneId);
    }

    @Transactional
    public OfferingResponse addSessions(UUID offeringId, AddSessionsRequest request) {
        Offering offering = offeringRepository.findById(offeringId)
                .orElseThrow(() -> new ResourceNotFoundException("Offering not found with ID: " + offeringId));

        List<SessionRequest> sessionRequests = request.sessions();

        // 1. Validate chronological bounds and internal overlaps in the service layer
        validateSessionRequests(sessionRequests);

        // 2. Map and save sessions
        List<Session> newSessions = new ArrayList<>();
        for (SessionRequest req : sessionRequests) {
            Session session = SessionMapper.toEntity(UUID.randomUUID(), offering, req);
            newSessions.add(session);
        }

        sessionRepository.saveAll(newSessions);

        // Update offering entity sessions list to return in response
        offering.getSessions().addAll(newSessions);

        ZoneId teacherZoneId = ZoneId.of(offering.getTeacher().getTimezone());
        return OfferingMapper.toResponse(offering, teacherZoneId);
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> getTeacherOfferings(UUID teacherId) {
        Teacher teacher = teacherRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("Teacher not found with ID: " + teacherId));

        List<Offering> offerings = offeringRepository.findByTeacherId(teacherId);
        ZoneId teacherZoneId = ZoneId.of(teacher.getTimezone());

        return offerings.stream()
                .map(offering -> OfferingMapper.toResponse(offering, teacherZoneId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OfferingResponse> getAvailableOfferings(String parentTimezone) {
        ZoneId clientZoneId;
        try {
            clientZoneId = ZoneId.of(parentTimezone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid parent timezone: " + parentTimezone);
        }

        List<Offering> offerings = offeringRepository.findAll();

        return offerings.stream()
                .map(offering -> OfferingMapper.toResponse(offering, clientZoneId))
                .toList();
    }

    private void validateSessionRequests(List<SessionRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return;
        }

        // 1. Verify individual session bounds (O(N))
        for (SessionRequest s : requests) {
            if (s.startTime() != null && s.endTime() != null && !s.endTime().isAfter(s.startTime())) {
                throw new IllegalArgumentException("Session end time must be after start time");
            }
        }

        // 2. Sort sessions copy by start time and check adjacent elements (O(N log N))
        List<SessionRequest> sorted = requests.stream()
                .filter(s -> s.startTime() != null && s.endTime() != null)
                .sorted(Comparator.comparing(SessionRequest::startTime))
                .toList();

        for (int i = 0; i < sorted.size() - 1; i++) {
            SessionRequest current = sorted.get(i);
            SessionRequest next = sorted.get(i + 1);

            if (current.endTime().isAfter(next.startTime())) {
                throw new IllegalArgumentException(String.format(
                        "Overlapping sessions detected in request payload: [%s - %s] overlaps with [%s - %s]",
                        current.startTime(), current.endTime(), next.startTime(), next.endTime()
                ));
            }
        }
    }
}
