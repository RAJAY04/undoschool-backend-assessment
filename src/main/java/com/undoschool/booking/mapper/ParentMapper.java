package com.undoschool.booking.mapper;

import com.undoschool.booking.dto.request.CreateParentRequest;
import com.undoschool.booking.dto.response.ParentResponse;
import com.undoschool.booking.entity.Parent;

import java.util.UUID;

public final class ParentMapper {

    private ParentMapper() {
        // Utility class
    }

    public static Parent toEntity(UUID id, CreateParentRequest request) {
        return Parent.builder()
                .id(id)
                .name(request.name())
                .email(request.email())
                .timezone(request.timezone())
                .build();
    }

    public static ParentResponse toResponse(Parent parent) {
        if (parent == null) {
            return null;
        }
        return new ParentResponse(
                parent.getId(),
                parent.getName(),
                parent.getEmail(),
                parent.getTimezone()
        );
    }
}
