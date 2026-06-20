package de.thm.swtp.api.professorRequest.service;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequest;
import de.thm.swtp.api.professorRequest.domain.ProfessorRequestStatus;
import de.thm.swtp.api.professorRequest.entity.ProfessorRequestEntity;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestAlreadyExistsException;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestInvalidStatusException;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestNotFoundException;
import de.thm.swtp.api.professorRequest.mapper.ProfessorRequestMapper;
import de.thm.swtp.api.professorRequest.repository.ProfessorRequestRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/** Service for managing professor-rights requests. */
@Service
@RequiredArgsConstructor
public class ProfessorRequestService {
    private final ProfessorRequestRepository professorRequestRepository;
    private final UserProfileRepository userProfileRepository;

    /** Creates a new professor-rights request with status PENDING for the given user. */
    @Transactional
    public ProfessorRequest createProfessorRequest(UUID currentUserId, String name, String email, String text) {
        if (professorRequestRepository.existsByRequestingUserKeycloakIdAndStatus(
                currentUserId, ProfessorRequestStatus.PENDING)) {
            throw new ProfessorRequestAlreadyExistsException(currentUserId);
        }

        UserProfile requestingUser = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));

        ProfessorRequestEntity entity = ProfessorRequestEntity.builder()
                .requestingUser(requestingUser)
                .name(name)
                .email(email)
                .text(text)
                .build();

        ProfessorRequestEntity saved = professorRequestRepository.save(entity);
        return ProfessorRequestMapper.toDomain(saved);
    }

    /** Returns a paginated list of all professor-rights requests. */
    @Transactional(readOnly = true)
    public Page<ProfessorRequest> getAllProfessorRequests(Pageable pageable) {
        return professorRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ProfessorRequestMapper::toDomain);
    }

    /** Accepts a professor-rights request by setting its status to ACCEPTED. */
    @Transactional
    public ProfessorRequest acceptProfessorRequest(UUID requestId) {
        ProfessorRequestEntity entity = professorRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProfessorRequestNotFoundException(requestId));

        if (entity.getStatus() != ProfessorRequestStatus.PENDING) {
            throw new ProfessorRequestInvalidStatusException(
                    "Only a pending professor request can be accepted. Current status: " + entity.getStatus());
        }

        entity.setStatus(ProfessorRequestStatus.ACCEPTED);
        ProfessorRequestEntity saved = professorRequestRepository.save(entity);
        return ProfessorRequestMapper.toDomain(saved);
    }

    /** Rejects a professor-rights request by setting its status to REJECTED. */
    @Transactional
    public ProfessorRequest rejectProfessorRequest(UUID requestId) {
        ProfessorRequestEntity entity = professorRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProfessorRequestNotFoundException(requestId));

        if (entity.getStatus() != ProfessorRequestStatus.PENDING) {
            throw new ProfessorRequestInvalidStatusException(
                    "Only a pending professor request can be rejected. Current status: " + entity.getStatus());
        }

        entity.setStatus(ProfessorRequestStatus.REJECTED);
        ProfessorRequestEntity saved = professorRequestRepository.save(entity);
        return ProfessorRequestMapper.toDomain(saved);
    }
}
