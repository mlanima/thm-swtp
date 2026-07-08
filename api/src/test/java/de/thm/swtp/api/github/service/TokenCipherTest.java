package de.thm.swtp.api.github.service;

import de.thm.swtp.api.github.config.GithubOAuthProperties;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenCipherTest {

    private static final String TEST_KEY = Base64.getEncoder().encodeToString(new byte[32]);

    private TokenCipher cipherWithKey(String key) {
        return new TokenCipher(new GithubOAuthProperties(
                "client-id", "client-secret", "redirect-uri", "read:user",
                "authorize-url", "token-url", key));
    }

    @Test
    void shouldRoundtripPlaintext() {
        var cipher = cipherWithKey(TEST_KEY);
        var ciphertext = cipher.encrypt("gho_secretToken");
        assertThat(cipher.decrypt(ciphertext)).isEqualTo("gho_secretToken");
    }

    @Test
    void shouldProduceDistinctCiphertextsForSamePlaintext() {
        var cipher = cipherWithKey(TEST_KEY);
        assertThat(cipher.encrypt("gho_secretToken")).isNotEqualTo(cipher.encrypt("gho_secretToken"));
    }

    @Test
    void shouldThrowWhenCiphertextTampered() {
        var cipher = cipherWithKey(TEST_KEY);
        var ciphertext = cipher.encrypt("gho_secretToken");
        byte[] bytes = Base64.getDecoder().decode(ciphertext);
        bytes[bytes.length - 1] ^= 0x1;
        var tampered = Base64.getEncoder().encodeToString(bytes);

        assertThatThrownBy(() -> cipher.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldThrowWhenKeyNotConfigured() {
        var cipher = cipherWithKey("");
        assertThatThrownBy(() -> cipher.encrypt("token")).isInstanceOf(IllegalStateException.class);
    }
}
