package de.thm.swtp.api.links.service;


import de.thm.swtp.api.exceptionhandling.exceptions.*;
import de.thm.swtp.api.links.domain.UserProfileLink;
import de.thm.swtp.api.links.entity.ProjectLinkEntity;
import de.thm.swtp.api.links.entity.UserProfileLinkEntity;
import de.thm.swtp.api.links.mapper.ProjectLinkMapper;
import de.thm.swtp.api.links.mapper.UserProfileLinkMapper;
import de.thm.swtp.api.links.repository.UserProfileLinkRepository;
import de.thm.swtp.api.userprofile.entity.UserProfile;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserProfileLinkService {
    private final UserProfileLinkRepository userProfileLinkRepository;
    private final UserProfileRepository userProfileRepository;

    @Transactional(readOnly = true)
    public List<UserProfileLink> getUserProfileLinks(UUID userProfileId) {
        getUserProfileOrThrowError(userProfileId);

        return userProfileLinkRepository.findByUserProfileKeycloakIdOrderByCreatedAtAsc(userProfileId)
                .stream()
                .map(UserProfileLinkMapper::toDomain)
                .toList();
    }

    @Transactional
    public UserProfileLink createUserProfileLink(UUID userProfileId, UUID currentUserId, String label, String url) {
        UserProfile userProfile = getUserProfileOrThrowError(userProfileId);
        checkUserProfileOwner(userProfile,currentUserId);


        String cleanedLabel = label.trim();
        String cleanedUrl = url.trim();

        if (userProfileLinkRepository.existsByUserProfileKeycloakIdAndUrlIgnoreCase(userProfile.getKeycloakId(), cleanedUrl)) {
            throw new UserProfileLinkAlreadyExistsException("The link already exists for this user profile.");
        }

        UserProfileLinkEntity userProfileLinkEntity = UserProfileLinkEntity.builder()
                .userProfile(userProfile)
                .label(cleanedLabel)
                .url(cleanedUrl)
                .build();

        UserProfileLinkEntity saved = userProfileLinkRepository.save(userProfileLinkEntity);
        return UserProfileLinkMapper.toDomain(saved);

    }

    @Transactional
    public UserProfileLink updateUserProfileLink(UUID userProfileId, UUID currentUserId, UUID linkId, String label, String url) {
        UserProfile userProfile = getUserProfileOrThrowError(userProfileId);
        checkUserProfileOwner(userProfile,currentUserId);

        UserProfileLinkEntity userProfileLinkEntity = getUserProfileLinkOrThrowError(linkId);
        checkLinkBelongsToUserProfile(userProfileLinkEntity,userProfileId);

        if (label != null) {
            userProfileLinkEntity.setLabel(label.trim());
        }

        if (url != null) {
            String cleanedUrl = url.trim();
            boolean changedUrl = !userProfileLinkEntity.getUrl().equals(cleanedUrl);

            if (changedUrl && userProfileLinkRepository.existsByUserProfileKeycloakIdAndUrlIgnoreCase(userProfile.getKeycloakId(), cleanedUrl)) {
                throw new UserProfileLinkAlreadyExistsException("The link already exists for this user profile.");
            }

            userProfileLinkEntity.setUrl(cleanedUrl);
        }

        UserProfileLinkEntity saved = userProfileLinkRepository.save(userProfileLinkEntity);
        return UserProfileLinkMapper.toDomain(saved);
    }

    @Transactional
    public void deleteUserProfileLink(UUID userProfileId, UUID currentUserId, UUID linkId) {
        UserProfile userprofile = getUserProfileOrThrowError(userProfileId);
        checkUserProfileOwner(userprofile,currentUserId);

        UserProfileLinkEntity userProfileLinkEntity = getUserProfileLinkOrThrowError(linkId);
        checkLinkBelongsToUserProfile(userProfileLinkEntity,userProfileId);

        userProfileLinkRepository.delete(userProfileLinkEntity);
    }





    private UserProfile getUserProfileOrThrowError(UUID userProfileId){
        return userProfileRepository.findById(userProfileId)
                .orElseThrow(() -> new UserProfileNotFoundException(userProfileId.toString()));
    }

    private void checkUserProfileOwner(UserProfile userProfile, UUID currentUserId){
        if (!userProfile.getKeycloakId().equals(currentUserId)) {
            throw new UserProfileLinkEditNotAllowedException("You are not allowed to edit this user profile");
        }
    }

    private UserProfileLinkEntity getUserProfileLinkOrThrowError(UUID linkId){
        return userProfileLinkRepository.findById(linkId)
                .orElseThrow(() -> new UserProfileLinkNotFoundException("Link: " + linkId + " could not be found."));
    }

    private void checkLinkBelongsToUserProfile(UserProfileLinkEntity userProfileLinkEntity, UUID userProfileId){
        if (!userProfileLinkEntity.getUserProfile().getKeycloakId().equals(userProfileId)){
            throw new UserProfileLinkDoesNotBelongToProfileException("Link does not belong to this user profile");
        }
    }
}
