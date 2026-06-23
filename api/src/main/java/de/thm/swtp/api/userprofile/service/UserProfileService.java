package de.thm.swtp.api.userprofile.service;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
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
                    return userProfileRepository.save(existing);
                })
                .orElseGet(() -> userProfileRepository.save(
                        UserProfile.builder()
                                .keycloakId(keycloakId)
                                .username(username)
                                .email(email)
                                .build()
                ));
    }

    @Transactional
    public UserProfile updateProfile(String username, String title, String location, String about, String experience) {
        UserProfile profile = findOrThrow(username);
        profile.setTitle(title);
        profile.setLocation(location);
        profile.setAbout(about);
        profile.setExperience(experience);
        return userProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(String username) {
        UserProfile profile = findOrThrow(username);
        // TODO: will throw FK constraint violation if the user owns projects — handle cascade or block deletion first
        userProfileRepository.delete(profile);
    }

    private UserProfile findOrThrow(String username) {
        return userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new UserProfileNotFoundException(username));
    }
}
