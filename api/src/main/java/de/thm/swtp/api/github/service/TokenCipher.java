package de.thm.swtp.api.github.service;

import de.thm.swtp.api.github.config.GithubOAuthProperties;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/** Encrypts/decrypts GitHub access tokens at rest with AES-256-GCM, keyed by
 * {@code github.oauth.token-encryption-key}. Output is Base64(12-byte random IV || ciphertext+tag). */
@Component
public class TokenCipher {

    private static final String CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private final SecretKeySpec keySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public TokenCipher(GithubOAuthProperties properties) {
        this.keySpec = buildKeySpec(properties.tokenEncryptionKey());
    }

    public String encrypt(String plaintext) {
        requireKey();
        byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt GitHub token", e);
        }
    }

    public String decrypt(String encoded) {
        requireKey();
        byte[] combined = Base64.getDecoder().decode(encoded);
        byte[] iv = Arrays.copyOfRange(combined, 0, GCM_IV_LENGTH_BYTES);
        byte[] ciphertext = Arrays.copyOfRange(combined, GCM_IV_LENGTH_BYTES, combined.length);
        try {
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt GitHub token", e);
        }
    }

    private static SecretKeySpec buildKeySpec(String base64Key) {
        if (base64Key == null || base64Key.isBlank()) {
            return null;
        }
        return new SecretKeySpec(Base64.getDecoder().decode(base64Key), "AES");
    }

    private void requireKey() {
        if (keySpec == null) {
            throw new IllegalStateException("GitHub token encryption key is not configured");
        }
    }
}
