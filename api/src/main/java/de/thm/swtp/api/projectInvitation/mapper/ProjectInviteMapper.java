package de.thm.swtp.api.projectInvitation.mapper;

import de.thm.swtp.api.discord.service.DiscordProjectService;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import org.springframework.stereotype.Component;

/** Maps between {@link ProjectInviteEntity} and {@link ProjectInvite} domain objects.*/
@Component
public class ProjectInviteMapper {

    private final DiscordProjectService discordProjectService;

    public ProjectInviteMapper(DiscordProjectService discordProjectService) {
        this.discordProjectService = discordProjectService;
    }

    /** Converts a project invitation entity into a domain object.*/
    public ProjectInvite toDomain(ProjectInviteEntity projectInviteEntity) {
        String discordInviteUrl = discordProjectService.getActiveInviteUrl(projectInviteEntity.getProject().getId());
        return ProjectInvite.builder()
                .id(projectInviteEntity.getId())
                .projectId(projectInviteEntity.getProject().getId())
                .projectName(projectInviteEntity.getProject().getName())
                .projectUrl(projectInviteEntity.getProject().getProjectUrl())
                .invitedByUsername(projectInviteEntity.getProject().getOwner().getUsername())
                .invitedUserId(projectInviteEntity.getInvitedUser().getKeycloakId())
                .message(projectInviteEntity.getMessage())
                .createdAt(projectInviteEntity.getCreatedAt())
                .status(projectInviteEntity.getStatus())
                .discordInviteUrl(discordInviteUrl)
                .build();
    }
}
