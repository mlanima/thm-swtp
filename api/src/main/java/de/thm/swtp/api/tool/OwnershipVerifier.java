package de.thm.swtp.api.tool;


import de.thm.swtp.api.exceptionhandling.exceptions.ProfileAccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

public class OwnershipVerifier {

    public static void verify(String username, Jwt jwt) {
        if (!jwt.getClaimAsString("preferred_username").equals(username)) {
            throw new ProfileAccessDeniedException();
        }
    }
}
