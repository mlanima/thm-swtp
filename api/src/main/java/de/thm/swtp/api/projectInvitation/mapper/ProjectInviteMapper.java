package de.thm.swtp.api.projectInvitation.mapper;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import de.thm.swtp.api.projectInvitation.domain.ProjectInvite;
import de.thm.swtp.api.projectInvitation.entity.ProjectInviteEntity;
import org.springframework.stereotype.Component;

/** Maps between {@link ProjectInviteEntity} and {@link ProjectInvite} domain objects.*/
@Component
public class ProjectInviteMapper {

    private final LinkedChannelRepository linkedChannelRepository;

    public ProjectInviteMapper(LinkedChannelRepository linkedChannelRepository) {
        this.linkedChannelRepository = linkedChannelRepository;
    }

    /** Converts a project invitation entity into a domain object.*/
    public ProjectInvite toDomain(ProjectInviteEntity projectInviteEntity) {
        String discordInviteUrl = linkedChannelRepository.findByProjectId(projectInviteEntity.getProject().getId())
                .filter(LinkedChannelEntity::isActive)
                .map(LinkedChannelEntity::getDiscordInviteUrl)
                .orElse(null);
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
