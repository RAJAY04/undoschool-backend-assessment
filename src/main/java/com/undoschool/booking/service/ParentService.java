package com.undoschool.booking.service;

import com.undoschool.booking.dto.request.CreateParentRequest;
import com.undoschool.booking.dto.response.ParentResponse;
import com.undoschool.booking.entity.Parent;
import com.undoschool.booking.mapper.ParentMapper;
import com.undoschool.booking.repository.ParentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository parentRepository;

    @Transactional
    public ParentResponse createParent(CreateParentRequest request) {
        // Automatically throws DateTimeException (mapped to 400 Bad Request) if invalid
        ZoneId.of(request.timezone());

        Parent parent = ParentMapper.toEntity(UUID.randomUUID(), request);
        Parent saved = parentRepository.save(parent);
        return ParentMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ParentResponse> getAllParents() {
        return parentRepository.findAll()
                .stream()
                .map(ParentMapper::toResponse)
                .toList();
    }
}
