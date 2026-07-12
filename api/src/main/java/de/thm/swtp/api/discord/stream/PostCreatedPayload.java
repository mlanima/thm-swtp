package de.thm.swtp.api.discord.stream;

import java.util.UUID;

public record PostCreatedPayload(
    UUID postId,
    UUID projectId,
    String content,
    String title,
    String authorName,
    String authorAvatar,
    String platformUrl
) {}
