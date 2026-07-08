package de.thm.swtp.api.github.service;

import de.thm.swtp.api.github.config.GithubOAuthProperties;
import de.thm.swtp.api.github.exception.InvalidGithubStateException;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

/** Mints and validates the OAuth {@code state} parameter without server-side storage: the
 * payload (userId, expiry, nonce) is HMAC-signed with a key derived from the token-encryption
 * secret, so validity can be checked on the completing request alone. */
@Component
public class GithubStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int NONCE_LENGTH_BYTES = 16;

    private final SecretKeySpec hmacKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public GithubStateService(GithubOAuthProperties properties) {
        this.hmacKey = deriveHmacKey(properties.tokenEncryptionKey());
    }

    public String create(UUID userId) {
        requireKey();
        long expiresAt = Instant.now().plus(STATE_TTL).getEpochSecond();
        byte[] nonce = new byte[NONCE_LENGTH_BYTES];
        secureRandom.nextBytes(nonce);
        String payload = userId + ":" + expiresAt + ":" + base64Url(nonce);
        return base64Url(payload.getBytes(StandardCharsets.UTF_8)) + "." + base64Url(hmac(payload));
    }

    public void validate(String state, UUID currentUserId) {
        requireKey();
        if (state == null || state.isBlank() || !state.contains(".")) {
            throw new InvalidGithubStateException("Malformed state parameter");
        }

        int separatorIndex = state.lastIndexOf('.');
        String encodedPayload = state.substring(0, separatorIndex);
        String providedSignature = state.substring(separatorIndex + 1);

        String payload = decodeBase64Url(encodedPayload);
        String expectedSignature = base64Url(hmac(payload));
        if (!MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8),
                providedSignature.getBytes(StandardCharsets.UTF_8))) {
            throw new InvalidGithubStateException("Invalid state signature");
        }

        String[] parts = payload.split(":", 3);
        if (parts.length != 3) {
            throw new InvalidGithubStateException("Malformed state payload");
        }

        UUID stateUserId = parseUuid(parts[0]);
        long expiresAt = parseEpochSeconds(parts[1]);

        if (Instant.now().getEpochSecond() > expiresAt) {
            throw new InvalidGithubStateException("State parameter expired");
        }
        if (!stateUserId.equals(currentUserId)) {
            throw new InvalidGithubStateException("State parameter does not match current user");
        }
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidGithubStateException("Malformed state payload");
        }
    }

    private static long parseEpochSeconds(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new InvalidGithubStateException("Malformed state payload");
        }
    }

    private static String decodeBase64Url(String encoded) {
        try {
            return new String(Base64.getUrlDecoder().decode(encoded), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new InvalidGithubStateException("Malformed state parameter");
        }
    }

    private byte[] hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(hmacKey);
            return mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign GitHub OAuth state", e);
        }
    }

    private static String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static SecretKeySpec deriveHmacKey(String base64EncryptionKey) {
        if (base64EncryptionKey == null || base64EncryptionKey.isBlank()) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("github-state:".getBytes(StandardCharsets.UTF_8));
            byte[] derived = digest.digest(Base64.getDecoder().decode(base64EncryptionKey));
            return new SecretKeySpec(derived, HMAC_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Failed to derive GitHub state signing key", e);
        }
    }

    private void requireKey() {
        if (hmacKey == null) {
            throw new IllegalStateException("GitHub token encryption key is not configured");
        }
    }
}
