package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.entity.LinkedChannelEntity;
import de.thm.swtp.api.discord.repository.LinkedChannelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DiscordProjectService {

    private final LinkedChannelRepository linkedChannelRepository;
    private final DiscordNotificationService discordNotificationService;

    public LinkedChannelEntity findChannelByProjectId(UUID projectId) {
        return linkedChannelRepository.findByProjectId(projectId).orElse(null);
    }

    public String getActiveInviteUrl(UUID projectId) {
        return linkedChannelRepository.findByProjectId(projectId)
                .filter(LinkedChannelEntity::isActive)
                .map(LinkedChannelEntity::getDiscordInviteUrl)
                .orElse(null);
    }

    public void notifyMemberLeft(UUID projectId, String projectName, String memberName) {
        discordNotificationService.notifyMemberLeft(projectId, projectName, memberName);
    }
}
