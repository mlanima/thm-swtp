package de.thm.swtp.api.tag.controller;

import de.thm.swtp.api.tag.dto.CreateTagRequest;
import de.thm.swtp.api.tag.dto.TagResponse;
import de.thm.swtp.api.tag.service.ProfileTagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class ProfileTagController {

    private final ProfileTagService profileTagService;

    /** Returns a list of all tags assigned to the given user profile.*/
    @GetMapping("/{userId}/profile/tags")
    public List<TagResponse> getProfileTags(@PathVariable UUID userId){
        return profileTagService.getUserProfileTags(userId)
                .stream()
                .map(TagResponse::toResponse)
                .toList();
    }

    /** Adds a tag to the currently authenticated users profile.*/
    @PostMapping("/me/profile/tags")
    public TagResponse addTagToProfile(@Valid @RequestBody CreateTagRequest request, @AuthenticationPrincipal Jwt jwt){
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return TagResponse.toResponse(profileTagService.addTagToProfile(currentUserId, request.name()));
    }
}
