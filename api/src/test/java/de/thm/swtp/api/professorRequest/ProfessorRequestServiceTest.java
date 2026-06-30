package de.thm.swtp.api.professorRequest;

import de.thm.swtp.api.professorRequest.domain.ProfessorRequest;
import de.thm.swtp.api.professorRequest.domain.ProfessorRequestStatus;
import de.thm.swtp.api.professorRequest.entity.ProfessorRequestEntity;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestInvalidStatusException;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestNotFoundException;
import de.thm.swtp.api.professorRequest.repository.ProfessorRequestRepository;
import de.thm.swtp.api.professorRequest.service.ProfessorRequestService;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProfessorRequestServiceTest {

    private UUID requestId;
    private UUID userId;

    private UserProfile user;

    private ProfessorRequestRepository professorRequestRepository;
    private UserProfileRepository userProfileRepository;
    private ProfessorRequestService professorRequestService;

    @BeforeEach
    void setUp() {
        professorRequestRepository = mock(ProfessorRequestRepository.class);
        userProfileRepository = mock(UserProfileRepository.class);

        professorRequestService = new ProfessorRequestService(
                professorRequestRepository,
                userProfileRepository
        );

        requestId = UUID.randomUUID();
        userId = UUID.randomUUID();

        user = new UserProfile();
        user.setKeycloakId(userId);
        user.setUsername("testuser");
    }

    String email = "testuser@example.com";
    String text = "I would like professor rights to create courses.";

    @Test
    void createProfessorRequest_shouldCreateRequest_whenUserExists() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.of(user));

        when(professorRequestRepository.save(any(ProfessorRequestEntity.class)))
                .thenAnswer(invocation -> {
                    ProfessorRequestEntity entity = invocation.getArgument(0);
                    entity.setId(requestId);
                    entity.setCreatedAt(LocalDateTime.now());
                    entity.setUpdatedAt(LocalDateTime.now());
                    entity.setStatus(ProfessorRequestStatus.PENDING);
                    return entity;
                });

        ProfessorRequest result = professorRequestService.createProfessorRequest(userId, email, text);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(requestId);
        assertThat(result.getRequestingUserId()).isEqualTo(userId);
        assertThat(result.getRequestingUsername()).isEqualTo("testuser");
        assertThat(result.getEmail()).isEqualTo(email);
        assertThat(result.getText()).isEqualTo(text);
        assertThat(result.getStatus()).isEqualTo(ProfessorRequestStatus.PENDING);

        verify(professorRequestRepository).save(any(ProfessorRequestEntity.class));
    }

    @Test
    void createProfessorRequest_shouldThrowException_whenUserNotFound() {
        when(userProfileRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professorRequestService.createProfessorRequest(userId, email, text))
                .isInstanceOf(UserProfileNotFoundException.class);

        verify(professorRequestRepository, never()).save(any());
    }

    @Test
    void getAllProfessorRequests_shouldReturnPaginatedResults() {
        ProfessorRequestEntity entity = ProfessorRequestEntity.builder()
                .id(requestId)
                .requestingUser(user)
                .email(email)
                .text(text)
                .status(ProfessorRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 20);
        Page<ProfessorRequestEntity> entityPage = new PageImpl<>(List.of(entity), pageable, 1);

        when(professorRequestRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(entityPage);

        Page<ProfessorRequest> result = professorRequestService.getAllProfessorRequests(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().getId()).isEqualTo(requestId);
        assertThat(result.getContent().getFirst().getRequestingUsername()).isEqualTo("testuser");
        assertThat(result.getContent().getFirst().getStatus()).isEqualTo(ProfessorRequestStatus.PENDING);

        verify(professorRequestRepository).findAllByOrderByCreatedAtDesc(pageable);
    }

    @Test
    void getAllProfessorRequests_shouldReturnEmptyPage_whenNoRequestsExist() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<ProfessorRequestEntity> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(professorRequestRepository.findAllByOrderByCreatedAtDesc(pageable)).thenReturn(emptyPage);

        Page<ProfessorRequest> result = professorRequestService.getAllProfessorRequests(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void acceptProfessorRequest_shouldSetStatusAccepted_whenRequestIsPending() {
        ProfessorRequestEntity entity = ProfessorRequestEntity.builder()
                .id(requestId)
                .requestingUser(user)
                .email(email)
                .text(text)
                .status(ProfessorRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(professorRequestRepository.findById(requestId)).thenReturn(Optional.of(entity));
        when(professorRequestRepository.save(entity)).thenReturn(entity);

        ProfessorRequest result = professorRequestService.acceptProfessorRequest(requestId);

        assertThat(result.getStatus()).isEqualTo(ProfessorRequestStatus.ACCEPTED);

        verify(professorRequestRepository).save(entity);
    }

    @Test
    void acceptProfessorRequest_shouldThrowNotFoundException_whenRequestDoesNotExist() {
        when(professorRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professorRequestService.acceptProfessorRequest(requestId))
                .isInstanceOf(ProfessorRequestNotFoundException.class);

        verify(professorRequestRepository, never()).save(any());
    }

    @Test
    void acceptProfessorRequest_shouldThrowInvalidStatusException_whenRequestIsNotPending() {
        ProfessorRequestEntity entity = ProfessorRequestEntity.builder()
                .id(requestId)
                .requestingUser(user)
                .email(email)
                .text(text)
                .status(ProfessorRequestStatus.REJECTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(professorRequestRepository.findById(requestId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> professorRequestService.acceptProfessorRequest(requestId))
                .isInstanceOf(ProfessorRequestInvalidStatusException.class);

        verify(professorRequestRepository, never()).save(any());
    }

    @Test
    void rejectProfessorRequest_shouldSetStatusRejected_whenRequestIsPending() {
        ProfessorRequestEntity entity = ProfessorRequestEntity.builder()
                .id(requestId)
                .requestingUser(user)
                .email(email)
                .text(text)
                .status(ProfessorRequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(professorRequestRepository.findById(requestId)).thenReturn(Optional.of(entity));
        when(professorRequestRepository.save(entity)).thenReturn(entity);

        ProfessorRequest result = professorRequestService.rejectProfessorRequest(requestId);

        assertThat(result.getStatus()).isEqualTo(ProfessorRequestStatus.REJECTED);

        verify(professorRequestRepository).save(entity);
    }

    @Test
    void rejectProfessorRequest_shouldThrowNotFoundException_whenRequestDoesNotExist() {
        when(professorRequestRepository.findById(requestId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> professorRequestService.rejectProfessorRequest(requestId))
                .isInstanceOf(ProfessorRequestNotFoundException.class);

        verify(professorRequestRepository, never()).save(any());
    }

    @Test
    void rejectProfessorRequest_shouldThrowInvalidStatusException_whenRequestIsNotPending() {
        ProfessorRequestEntity entity = ProfessorRequestEntity.builder()
                .id(requestId)
                .requestingUser(user)
                .email(email)
                .text(text)
                .status(ProfessorRequestStatus.ACCEPTED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        when(professorRequestRepository.findById(requestId)).thenReturn(Optional.of(entity));

        assertThatThrownBy(() -> professorRequestService.rejectProfessorRequest(requestId))
                .isInstanceOf(ProfessorRequestInvalidStatusException.class);

        verify(professorRequestRepository, never()).save(any());
    }
}
