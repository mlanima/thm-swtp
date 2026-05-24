package de.thm.swtp.api.tag.profile;

import de.thm.swtp.api.tag.Tag;
import de.thm.swtp.api.tag.dto.AddProfileTagRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profiles/{profileId}/tags")
@RequiredArgsConstructor
public class ProfileTagController {

    private final ProfileTagService profileTagService;

    @PostMapping
    public ResponseEntity<Tag> addTag(@PathVariable String profileId,
                                       @RequestBody AddProfileTagRequest request) {
        return ResponseEntity.ok(profileTagService.addTagToProfile(profileId, request));
    }
}
