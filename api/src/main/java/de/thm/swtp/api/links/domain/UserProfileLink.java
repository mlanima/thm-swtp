package de.thm.swtp.api.links.domain;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class UserProfileLink {
    private UUID id;
    private UUID userProfileId;
    private String label;
    private String url;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
