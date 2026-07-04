package de.thm.swtp.api.moderation;

import de.thm.swtp.api.common.LogSafe;
import de.thm.swtp.api.moderation.exception.ContentModerationException;
import de.thm.swtp.api.moderation.exception.ContentNotValidException;
import de.thm.swtp.api.moderation.exception.ModerationApiException;
import de.thm.swtp.api.tag.validation.BlocklistService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Slf4j
@Service
public class ContentModerationService {

    private final ModerationClient moderationClient;
    private final BlocklistService blocklistService;
    private final boolean blocklistFallback;

    public ContentModerationService(
            final ModerationClient moderationClient,
            final BlocklistService blocklistService,
            @Value("${app.content-moderation.blocklist-fallback:true}") final boolean blocklistFallback) {
        this.moderationClient = moderationClient;
        this.blocklistService = blocklistService;
        this.blocklistFallback = blocklistFallback;
    }

    @Cacheable(value = "content-moderation", key = "#hash(content)")
    public boolean isContentAppropriate(final String content) {
        log.debug("Cache miss for content moderation — querying OpenAI");
        try {
            return !moderationClient.isFlagged(content);
        } catch (ModerationApiException e) {
            log.warn("OpenAI moderation unavailable, falling back to blocklist");
            return !blocklistService.containsAny(content);
        }
    }

    public void assertAppropriate(final String content, final String fieldName) {
        if (content == null || content.isBlank()) {
            return;
        }
        var appropriate = isContentAppropriate(content);
        if (appropriate) {
            log.info("Moderation '{}' passed: {}", fieldName, LogSafe.clean(content));
        } else {
            log.warn("Moderation '{}' rejected: {}", fieldName, LogSafe.clean(content));
            throw new ContentNotValidException(fieldName);
        }
    }

    public String hash(final String content) {
        try {
            var digest = MessageDigest.getInstance("SHA-256");
            var bytes = digest.digest((content != null ? content : "").getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}
