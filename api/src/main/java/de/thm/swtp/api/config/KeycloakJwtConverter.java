package de.thm.swtp.api.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(extractRoles(jwt));
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority("ROLE_USER"));

        for (String role : roles) {
            authorities.add(new SimpleGrantedAuthority("KEYCLOAK_" + role));
            if ("MODERATOR".equals(role)) {
                authorities.add(new SimpleGrantedAuthority("ROLE_MODERATOR"));
            }
        }

        return authorities;
    }
}
