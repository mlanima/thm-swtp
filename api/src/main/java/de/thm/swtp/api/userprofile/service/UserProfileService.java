package de.thm.swtp.api.userprofile.service;

import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import de.thm.swtp.api.auditlog.AuditLogService;
import de.thm.swtp.api.auditlog.AuditActor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final AuditLogService auditLogService;

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
                    // note: debug, not info/txlogger — this runs on every authenticated
                    // request (jwt sync), so info would be noise; and it's a sync, not a
                    // durability lifecycle claim, so txlogger (commit-gated info) doesn't fit.
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
    public UserProfile updateProfile(String username, String title, String location, String about, String experience) {
        UserProfile profile = findOrThrow(username);
        profile.setTitle(title);
        profile.setLocation(location);
        profile.setAbout(about);
        profile.setExperience(experience);
        UserProfile saved = userProfileRepository.save(profile);
        TxLogger.afterCommit(log, "Profile updated: user={}", username);
        return saved;
    }

    @Transactional
    public void deleteProfile(String username) {
        UserProfile profile = findOrThrow(username);
        // TODO: will throw FK constraint violation if the user owns projects — handle cascade or block deletion first
        userProfileRepository.delete(profile);
        TxLogger.afterCommit(log, "Profile deleted: user={}", username);
    }

    private UserProfile findOrThrow(String username) {
        return userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new UserProfileNotFoundException(username));
    }

    @Transactional(readOnly = true)
    public Page<UserProfile> getUsersByStatus(UserStatus status, Pageable pageable) {
        return userProfileRepository.findByStatus(status, pageable);
    }

    @Transactional
    public UserProfile banUser(UUID userId, String reason, AuditActor actor) {
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        userProfile.setStatus(UserStatus.BANNED);
        userProfile.setBanReason(reason);
        userProfile.setBannedAt(LocalDateTime.now());

        UserProfile saved =  userProfileRepository.save(userProfile);

        auditLogService.logUserBanned(
                actor,
                userId,
                saved.getUsername(),
                reason
        );

        TxLogger.afterCommit(log, "User banned: username={}, userId={}, actor={}", saved.getUsername(), userId, actor.userId());
        return saved;
    }

    @Transactional
    public UserProfile unbanUser(UUID userId, AuditActor actor) {
        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserProfileNotFoundException(userId.toString()));

        String username = userProfile.getUsername();

        userProfile.setStatus(UserStatus.ACTIVE);
        userProfile.setBanReason(null);
        userProfile.setBannedAt(null);

        UserProfile saved =  userProfileRepository.save(userProfile);

        auditLogService.logUserUnbanned(
                actor,
                userId,
                username
        );

        TxLogger.afterCommit(log, "User unbanned: username={}, userId={}, actor={}", username, userId, actor.userId());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<UserProfile> findProfileByKeycloakId(UUID keycloakId) {
        return userProfileRepository.findByKeycloakId(keycloakId);
    }
}
