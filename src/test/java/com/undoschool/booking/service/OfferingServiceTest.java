package com.undoschool.booking.service;

import com.undoschool.booking.dto.request.AddSessionsRequest;
import com.undoschool.booking.dto.request.SessionRequest;
import com.undoschool.booking.dto.response.OfferingResponse;
import com.undoschool.booking.entity.Course;
import com.undoschool.booking.entity.Offering;
import com.undoschool.booking.entity.Teacher;
import com.undoschool.booking.repository.CourseRepository;
import com.undoschool.booking.repository.OfferingRepository;
import com.undoschool.booking.repository.SessionRepository;
import com.undoschool.booking.repository.TeacherRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OfferingServiceTest {

    @Mock
    private OfferingRepository offeringRepository;

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private TeacherRepository teacherRepository;

    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private OfferingService offeringService;

    @Test
    void shouldPass_WhenSessionsAreChronologicalAndClean() {
        UUID offeringId = UUID.randomUUID();
        Offering stubOffering = createStubOffering(offeringId);

        SessionRequest session1 = new SessionRequest(
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T11:00:00Z")
        );
        SessionRequest session2 = new SessionRequest(
                Instant.parse("2026-06-01T11:00:00Z"),
                Instant.parse("2026-06-01T12:00:00Z")
        );
        AddSessionsRequest request = new AddSessionsRequest(List.of(session1, session2));

        // Stubbing: Return the mock offering upon lookup
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(stubOffering));

        // Stubbing: Return the saved sessions list
        when(sessionRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OfferingResponse response = offeringService.addSessions(offeringId, request);

        assertThat(response).isNotNull();
        assertThat(response.offeringId()).isEqualTo(offeringId);
        
        verify(sessionRepository, times(1)).saveAll(any());
    }

    @Test
    void shouldThrowException_WhenSessionEndTimeIsBeforeStartTime() {
        UUID offeringId = UUID.randomUUID();
        Offering stubOffering = createStubOffering(offeringId);

        SessionRequest invalidSession = new SessionRequest(
                Instant.parse("2026-06-01T11:00:00Z"),
                Instant.parse("2026-06-01T10:00:00Z")
        );
        AddSessionsRequest request = new AddSessionsRequest(List.of(invalidSession));

        // Stubbing: Return the mock offering upon lookup
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(stubOffering));

        assertThatThrownBy(() -> offeringService.addSessions(offeringId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Session end time must be after start time");

        verify(sessionRepository, never()).saveAll(any());
    }

    @Test
    void shouldThrowException_WhenSessionsChronologicallyOverlap() {
        UUID offeringId = UUID.randomUUID();
        Offering stubOffering = createStubOffering(offeringId);

        SessionRequest session1 = new SessionRequest(
                Instant.parse("2026-06-01T10:00:00Z"),
                Instant.parse("2026-06-01T11:15:00Z")
        );
        SessionRequest session2 = new SessionRequest(
                Instant.parse("2026-06-01T11:00:00Z"),
                Instant.parse("2026-06-01T12:00:00Z")
        );
        AddSessionsRequest request = new AddSessionsRequest(List.of(session1, session2));

        // Stubbing: Return the mock offering upon lookup
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(stubOffering));

        assertThatThrownBy(() -> offeringService.addSessions(offeringId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Overlapping sessions detected in request payload");

        verify(sessionRepository, never()).saveAll(any());
    }

    private Offering createStubOffering(UUID id) {
        Teacher teacher = Teacher.builder()
                .id(UUID.randomUUID())
                .name("Jane Doe")
                .email("jane.doe@undoschool.com")
                .timezone("Asia/Kolkata")
                .build();

        Course course = Course.builder()
                .id(UUID.randomUUID())
                .title("Intro to Java")
                .description("Learn Java from scratch")
                .build();

        return Offering.builder()
                .id(id)
                .teacher(teacher)
                .course(course)
                .title("Java Basics - Cohort 1")
                .maxStudents(10)
                .currentEnrollment(0)
                .sessions(new ArrayList<>())
                .build();
    }
}
