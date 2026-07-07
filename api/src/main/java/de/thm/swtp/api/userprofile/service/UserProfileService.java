package de.thm.swtp.api.userprofile.service;

import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.location.GooglePlacesClient;
import de.thm.swtp.api.location.exception.InvalidPlaceException;
import de.thm.swtp.api.moderation.ContentModerationService;
import de.thm.swtp.api.exceptionhandling.exceptions.InvalidUserManagementSortFieldException;
import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ContentModerationService contentModerationService;
    private final GooglePlacesClient googlePlacesClient;


    private static final Set<String> MANAGED_USER_SORT_FIELDS = Set.of("username", "email", "isProfessor", "createdAt", "bannedAt", "banReason", "status");

    @Transactional(readOnly = true)
    public UserProfile getProfile(String username) {
        return findOrThrow(username);
    }

    @Transactional
    public UserProfile getOrCreateProfile(UUID keycloakId, String username, String email) {
        return userProfileRepository.findById(keycloakId)
                .map(existing -> {
                    existing.setUsername(username);
                    existing.setEmail(email);
                    UserProfile synced = userProfileRepository.save(existing);
                    log.debug("Profile synced from JWT: user={}", username);
                    return synced;
                })
                .orElseGet(() -> {
                    UserProfile created = userProfileRepository.save(
                            UserProfile.builder()
                                    .keycloakId(keycloakId)
                                    .username(username)
                                    .email(email)
                                    .build()
                    );
                    TxLogger.afterCommit(log, "Profile created: user={}", username);
                    return created;
                });
    }

    @Transactional
    public UserProfile updateProfile(String username, String title, String location, String about, String experience, String placeId) {
        UserProfile profile = findOrThrow(username);

        if (title != null) {
            contentModerationService.assertAppropriate(title, "title");
            profile.setTitle(title);
        }
        if (about != null) {
            contentModerationService.assertAppropriate(about, "about");
            profile.setAbout(about);
        }
        if (experience != null) {
            contentModerationService.assertAppropriate(experience, "experience");
            profile.setExperience(experience);
        }
        if (location != null && placeId != null) {
            if (location.isBlank()) {
                profile.setLocation(null);
                profile.setPlaceId(null);
            } else if (placeId.isBlank()) {
                throw new InvalidPlaceException("Location provided without a valid placeId");
            } else {
                var validatedLocation = googlePlacesClient.validatePlaceId(placeId);
                profile.setLocation(validatedLocation);
                profile.setPlaceId(placeId);
            }
        }

        UserProfile saved = userProfileRepository.save(profile);
        TxLogger.afterCommit(log, "Profile updated: user={}", username);
        return saved;
    }

    @Transactional
    public void deleteProfile(String username) {
        UserProfile profile = findOrThrow(username);
        userProfileRepository.delete(profile);
        TxLogger.afterCommit(log, "Profile deleted: user={}", username);
    }

    private UserProfile findOrThrow(String username) {
        return userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new UserProfileNotFoundException(username));
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getUsersByStatus(UserStatus status, Pageable pageable) {
        validateManagedUserSort(pageable.getSort());
        return userProfileRepository.findByStatus(status, pageable);
    }

    @Transactional
    public UserProfile banUser(UUID userId, String reason){
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        userProfile.setStatus(UserStatus.BANNED);
        userProfile.setBanReason(reason);
        userProfile.setBannedAt(LocalDateTime.now());

        UserProfile saved =  userProfileRepository.save(userProfile);

        TxLogger.afterCommit(log, "User banned: username={}, userId={}", userProfile.getUsername(), userId);
        return saved;
    }

    @Transactional
    public UserProfile unbanUser(UUID userId){
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        userProfile.setStatus(UserStatus.ACTIVE);
        userProfile.setBanReason(null);
        userProfile.setBannedAt(null);

        UserProfile saved =  userProfileRepository.save(userProfile);
        TxLogger.afterCommit(log, "User unbanned: username={}, userId={}", userProfile.getUsername(), userId);
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> findProfileByKeycloakId(UUID keycloakId) {
        return userProfileRepository.findByKeycloakId(keycloakId);
    }

    @Transactional
    public void updateOnboardingCompleted(UUID currentUserId, boolean onboardingCompleted) {
        UserProfile userProfile = userProfileRepository.findById(currentUserId)
                .orElseThrow(() -> new UserProfileNotFoundException(currentUserId.toString()));

        userProfile.setOnboardingCompleted(onboardingCompleted);
        userProfileRepository.save(userProfile);
        TxLogger.afterCommit(log, "User completed onboarding: username={}, userId={}", userProfile.getUsername(), currentUserId);
    }

    private void validateManagedUserSort(Sort sort){
       sort.forEach((sortField) -> {
           if (!MANAGED_USER_SORT_FIELDS.contains(sortField.getProperty())) {
               throw new InvalidUserManagementSortFieldException("Unsupported sort field: " + sortField.getProperty());
           }
       });
    }
}
