package de.thm.swtp.api.userprofile.service;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public UserProfile getProfile(String keycloakId) {
        return findOrThrow(keycloakId);
    }

    @Transactional
    public UserProfile getOrCreateProfile(String keycloakId, String username, String email) {
        return userProfileRepository.findByKeycloakId(keycloakId)
                .orElseGet(() -> userProfileRepository.save(
                        UserProfile.builder()
                                .keycloakId(keycloakId)
                                .username(username)
                                .email(email)
                                .build()
                ));
    }

    @Transactional
    public UserProfile updateProfile(String keycloakId, String about, String experience) {
        UserProfile profile = findOrThrow(keycloakId);
        profile.setAbout(about);
        profile.setExperience(experience);
        return userProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(String keycloakId) {
        userProfileRepository.delete(findOrThrow(keycloakId));
    }

    private UserProfile findOrThrow(String keycloakId) {
        return userProfileRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new UserProfileNotFoundException(keycloakId));
    }
}
