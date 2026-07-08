package de.thm.swtp.api.github.service;

import de.thm.swtp.api.github.config.GithubOAuthProperties;
import de.thm.swtp.api.github.exception.InvalidGithubStateException;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GithubStateServiceTest {

    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private GithubStateService stateService() {
        return new GithubStateService(new GithubOAuthProperties(
                "client-id", "client-secret", "redirect-uri", "read:user",
                "authorize-url", "token-url", TEST_KEY));
    }

    @Test
    void shouldValidateFreshlyCreatedState() {
        var service = stateService();
        var userId = UUID.randomUUID();
        var state = service.create(userId);

        assertThatCode(() -> service.validate(state, userId)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectStateForDifferentUser() {
        var service = stateService();
        var state = service.create(UUID.randomUUID());

        assertThatThrownBy(() -> service.validate(state, UUID.randomUUID()))
                .isInstanceOf(InvalidGithubStateException.class);
    }

    @Test
    void shouldRejectTamperedState() {
        var service = stateService();
        var userId = UUID.randomUUID();
        var state = service.create(userId);
        var tampered = state.substring(0, state.length() - 1) + (state.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> service.validate(tampered, userId))
                .isInstanceOf(InvalidGithubStateException.class);
    }

    @Test
    void shouldRejectMalformedState() {
        var service = stateService();
        assertThatThrownBy(() -> service.validate("not-a-valid-state", UUID.randomUUID()))
                .isInstanceOf(InvalidGithubStateException.class);
    }

    @Test
    void shouldRejectExpiredState() throws Exception {
        var userId = UUID.randomUUID();
        var expiredPayload = userId + ":" + Instant.now().minus(Duration.ofMinutes(1)).getEpochSecond() + ":nonce";
        var state = signPayloadWithSameKeyDerivation(expiredPayload);

        assertThatThrownBy(() -> stateService().validate(state, userId))
                .isInstanceOf(InvalidGithubStateException.class);
    }

    /** Signs a payload using the same key-derivation and encoding {@link GithubStateService} uses,
     * so an expired-but-correctly-signed state can be constructed for the expiry test. */
    private String signPayloadWithSameKeyDerivation(String payload) throws Exception {
        var digest = MessageDigest.getInstance("SHA-256");
        digest.update("github-state:".getBytes(StandardCharsets.UTF_8));
        var derivedKey = digest.digest(Base64.getDecoder().decode(TEST_KEY));

        var mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(derivedKey, "HmacSHA256"));
        var signature = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));

        var encodedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        var encodedSignature = Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        return encodedPayload + "." + encodedSignature;
    }
}
