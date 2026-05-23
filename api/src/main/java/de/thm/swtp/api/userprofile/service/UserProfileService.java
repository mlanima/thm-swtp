package de.thm.swtp.api.userprofile.service;

import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import de.thm.swtp.api.users.entity.User;
import de.thm.swtp.api.users.exception.UserNotFoundException;
import de.thm.swtp.api.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserProfileService {

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public UserProfile getProfile(String userId) {
        User user = findUserOrThrow(userId);
        return findProfileOrThrow(user);
    }

    @Transactional
    public UserProfile updateProfile(String userId, String about, String experience) {
        User user = findUserOrThrow(userId);
        UserProfile profile = findProfileOrThrow(user);

        profile.setAbout(about);
        profile.setExperience(experience);

        return userProfileRepository.save(profile);
    }

    @Transactional
    public void deleteProfile(String userId) {
        User user = findUserOrThrow(userId);
        UserProfile profile = findProfileOrThrow(user);
        userProfileRepository.delete(profile);
    }

    private User findUserOrThrow(String userId) {
        return userRepository.findByKeycloakId(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private UserProfile findProfileOrThrow(User user) {
        return userProfileRepository.findByUser(user)
                .orElseThrow(() -> new UserProfileNotFoundException(user.getKeycloakId()));
    }
}
