package de.thm.swtp.api.professorRequest.service;

import de.thm.swtp.api.exceptionhandling.exceptions.InvalidProfessorEmailDomainException;
import de.thm.swtp.api.notification.event.ProfessorRequestVerificationCreatedEvent;
import de.thm.swtp.api.professorRequest.config.ProfessorRequestProperties;
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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/** Service for managing professor-rights requests. */
@Service
@RequiredArgsConstructor
public class ProfessorRequestService {
    private final ProfessorRequestRepository professorRequestRepository;
    private final UserProfileRepository userProfileRepository;
    private final ProfessorRequestProperties professorRequestProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Creates a new professor-rights request with status WAITING_EMAIL_VERIFICATION for the given user.
     */
    @Transactional
    public ProfessorRequest createProfessorRequest(UUID currentUserId, String email, String text) {
        String normalizedEmail = normalizeEmail(email);
        UserProfile requestingUser = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));

        if (!isAllowedProfessorEmail(normalizedEmail)) {
            throw new InvalidProfessorEmailDomainException();
        }

        expireOutdatedVerificationRequestsForUser(currentUserId);
        validateNoOpenProfessorRequest(currentUserId);

        String verificationToken = createVerificationToken();
        String verificationTokenHash = hashToken(verificationToken);

        ProfessorRequestEntity entity = ProfessorRequestEntity.builder()
                .requestingUser(requestingUser)
                .email(normalizedEmail)
                .text(text)
                .status(ProfessorRequestStatus.WAITING_EMAIL_VERIFICATION)
                .verificationTokenHash(verificationTokenHash)
                .verificationExpiresAt(LocalDateTime.now().plusHours(professorRequestProperties.verificationTokenValidHours()))
                .build();

        ProfessorRequestEntity saved = professorRequestRepository.save(entity);
        ProfessorRequest savedDomain = ProfessorRequestMapper.toDomain(saved);

        eventPublisher.publishEvent(new ProfessorRequestVerificationCreatedEvent(savedDomain, verificationToken));

        return savedDomain;
    }

    @Transactional
    public ProfessorRequest verifyProfessorRequestEmail(String token) {
        String tokenHash = hashToken(token);

        ProfessorRequestEntity professorRequestEntity = professorRequestRepository.findByVerificationTokenHash(tokenHash)
                .orElseThrow(() -> new ProfessorRequestInvalidStatusException("Invalid verification token."));

        if (professorRequestEntity.getStatus() != ProfessorRequestStatus.WAITING_EMAIL_VERIFICATION) {
            throw new ProfessorRequestInvalidStatusException("Only requests waiting for email verification can be verified. Current status: " + professorRequestEntity.getStatus());
        }

        if (professorRequestEntity.getVerificationExpiresAt() == null || professorRequestEntity.getVerificationExpiresAt().isBefore(LocalDateTime.now())) {
            professorRequestEntity.setStatus(ProfessorRequestStatus.EXPIRED);
            professorRequestEntity.setVerificationTokenHash(null);

            ProfessorRequestEntity saved = professorRequestRepository.save(professorRequestEntity);
            return ProfessorRequestMapper.toDomain(saved);
        }

        professorRequestEntity.setEmailVerifiedAt(LocalDateTime.now());
        professorRequestEntity.setVerificationTokenHash(null);
        professorRequestEntity.setStatus(ProfessorRequestStatus.PENDING);

        ProfessorRequestEntity saved = professorRequestRepository.save(professorRequestEntity);
        return ProfessorRequestMapper.toDomain(saved);
    }

    /**
     * Returns a paginated list of all professor-rights requests.
     */
    @Transactional(readOnly = true)
    public Page<ProfessorRequest> getAllProfessorRequests(Pageable pageable) {
        return professorRequestRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(ProfessorRequestMapper::toDomain);
    }

    /**
     * Returns all requests for the given user, ordered by newest first.
     */
    @Transactional(readOnly = true)
    public List<ProfessorRequest> getRequestsByUser(UUID userId) {
        return professorRequestRepository.findAllByRequestingUserKeycloakIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(ProfessorRequestMapper::toDomain)
                .toList();
    }

    /**
     * Accepts a professor-rights request by setting its status to ACCEPTED and granting the professor role.
     */
    @Transactional
    public ProfessorRequest acceptProfessorRequest(UUID requestId) {
        ProfessorRequestEntity entity = professorRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProfessorRequestNotFoundException(requestId));

        if (entity.getStatus() != ProfessorRequestStatus.PENDING) {
            throw new ProfessorRequestInvalidStatusException(
                    "Only a pending professor request can be accepted. Current status: " + entity.getStatus());
        }

        entity.setStatus(ProfessorRequestStatus.ACCEPTED);
        entity.setVerificationTokenHash(null);
        entity.getRequestingUser().setProfessor(true); // Grant professor role to the user
        ProfessorRequestEntity saved = professorRequestRepository.save(entity);
        return ProfessorRequestMapper.toDomain(saved);
    }

    /**
     * Rejects a professor-rights request by setting its status to REJECTED.
     */
    @Transactional
    public ProfessorRequest rejectProfessorRequest(UUID requestId) {
        ProfessorRequestEntity entity = professorRequestRepository.findById(requestId)
                .orElseThrow(() -> new ProfessorRequestNotFoundException(requestId));

        if (entity.getStatus() != ProfessorRequestStatus.PENDING && entity.getStatus() != ProfessorRequestStatus.WAITING_EMAIL_VERIFICATION) {
            throw new ProfessorRequestInvalidStatusException(
                    "Only a pending professor request can be rejected. Current status: " + entity.getStatus());
        }

        entity.setStatus(ProfessorRequestStatus.REJECTED);
        entity.setVerificationTokenHash(null);
        ProfessorRequestEntity saved = professorRequestRepository.save(entity);
        return ProfessorRequestMapper.toDomain(saved);
    }

    @Transactional
    public int expireOutdatedVerificationRequests() {
        List<ProfessorRequestEntity> expiredRequests = professorRequestRepository
                .findAllByStatusAndVerificationExpiresAtBefore(ProfessorRequestStatus.WAITING_EMAIL_VERIFICATION, LocalDateTime.now());

        expiredRequests.forEach(request -> {
            request.setStatus(ProfessorRequestStatus.EXPIRED);
            request.setVerificationTokenHash(null);
        });

        return expiredRequests.size();
    }


    private void expireOutdatedVerificationRequestsForUser(UUID userId) {
        List<ProfessorRequestEntity> expiredRequests =
                professorRequestRepository.findAllByRequestingUserKeycloakIdAndStatusAndVerificationExpiresAtBefore(
                        userId,
                        ProfessorRequestStatus.WAITING_EMAIL_VERIFICATION,
                        LocalDateTime.now()
                );

        expiredRequests.forEach(request -> {
            request.setStatus(ProfessorRequestStatus.EXPIRED);
            request.setVerificationTokenHash(null);
        });
    }


    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

    private boolean isAllowedProfessorEmail(String normalizedEmail) {
        return professorRequestProperties.allowedEmailDomains()
                .stream()
                .map(domain -> domain.trim().toLowerCase())
                .anyMatch(domain -> normalizedEmail.endsWith("@" + domain));
    }

    private void validateNoOpenProfessorRequest(UUID userId) {
        boolean openRequestExists = professorRequestRepository.existsByRequestingUserKeycloakIdAndStatusIn(
                userId,
                List.of(ProfessorRequestStatus.PENDING, ProfessorRequestStatus.WAITING_EMAIL_VERIFICATION)
        );

        if (openRequestExists) {
            throw new ProfessorRequestAlreadyExistsException(userId);
        }
    }

    private String createVerificationToken() {
        return UUID.randomUUID() + "-" + UUID.randomUUID();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}
