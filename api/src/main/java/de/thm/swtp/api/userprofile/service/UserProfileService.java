package de.thm.swtp.api.userprofile.service;

import de.thm.swtp.api.userAccount.domain.UserRole;
import de.thm.swtp.api.userAccount.entity.UserAccountEntity;
import de.thm.swtp.api.userAccount.repository.UserAccountRepository;
import de.thm.swtp.api.exceptionhandling.exceptions.UserProfileNotAllowedException;
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
    private final UserAccountRepository userAccountRepository;

    @Transactional(readOnly = true)
    public UserProfile getProfile(String username) {
        return findOrThrow(username);
    }

    @Transactional
    public UserProfile getOrCreateProfile(UUID keycloakId, String username, String email) {
        UserAccountEntity userAccountEntity = userAccountRepository.findById(keycloakId)
                .orElseGet(() -> userAccountRepository.save(
                        UserAccountEntity.builder()
                                .keycloakId(keycloakId)
                                .role(UserRole.USER)
                                .build()));

        if (userAccountEntity.getRole() == UserRole.ADMIN) {
            throw new UserProfileNotAllowedException("User profiles are not available for admin accounts.");
        }

        return userProfileRepository.findById(keycloakId)
                .map(existing -> {
                    existing.setUsername(username);
                    existing.setEmail(email);
                    return userProfileRepository.save(existing);
                })
                .orElseGet(() -> userProfileRepository.save(
                        UserProfile.builder()
                                .username(username)
                                .email(email)
                                .userAccount(userAccountEntity)
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
        UUID keycloakId = profile.getKeycloakId();
        // TODO: will throw FK constraint violation if the user owns projects — handle cascade or block deletion first
        userProfileRepository.delete(profile);
        userAccountRepository.deleteById(keycloakId);
    }

    private UserProfile findOrThrow(String username) {
        return userProfileRepository.findByUsername(username)
                .orElseThrow(() -> new UserProfileNotFoundException(username));
    }
}
