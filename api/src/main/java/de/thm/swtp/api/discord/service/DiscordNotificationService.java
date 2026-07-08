package de.thm.swtp.api.discord.service;

import de.thm.swtp.api.discord.stream.DiscordEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DiscordNotificationService {

    private final DiscordEventPublisher eventPublisher;

    public void notifyMemberJoined(UUID projectId, String projectName, String memberName) {
        eventPublisher.publishProjectEvent(
                projectId,
                "MEMBER_JOIN",
                memberName + " joined the project"
        );
    }

    public void notifyMemberLeft(UUID projectId, String projectName, String memberName) {
        eventPublisher.publishProjectEvent(
                projectId,
                "MEMBER_LEAVE",
                memberName + " left the project"
        );
    }

    public void notifyProjectStatusChanged(UUID projectId, String projectName, String newStatus) {
        eventPublisher.publishProjectEvent(
                projectId,
                "STATUS_CHANGED",
                "Project status changed to: " + newStatus
        );
    }
}
