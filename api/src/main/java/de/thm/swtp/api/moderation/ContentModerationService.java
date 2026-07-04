package de.thm.swtp.api.moderation;

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

    @Cacheable(value = "content-moderation", key = "#hash(content)", unless = "#result")
    public boolean isContentAppropriate(final String content) {
        try {
            return !moderationClient.isFlagged(content);
        } catch (ModerationApiException e) {
            if (blocklistFallback) {
                log.warn("OpenAI moderation unavailable, falling back to blocklist");
                return !blocklistService.containsAny(content);
            }
            throw new ContentModerationException("Content moderation temporarily unavailable");
        }
    }

    public void assertAppropriate(final String content, final String fieldName) {
        if (content == null || content.isBlank()) {
            return;
        }
        if (!isContentAppropriate(content)) {
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
