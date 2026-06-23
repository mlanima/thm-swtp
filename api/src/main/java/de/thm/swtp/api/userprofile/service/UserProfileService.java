package de.thm.swtp.api.userprofile.service;

import de.thm.swtp.api.common.TxLogger;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

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
}
