package de.thm.swtp.api.config;

import de.thm.swtp.api.userAccount.entity.UserAccountEntity;
import de.thm.swtp.api.userAccount.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class KeycloakJwtConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserAccountRepository userAccountRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>(extractRoles(jwt));
        Optional<GrantedAuthority> backendRole =
                getBackendRoleAuthority(jwt);
        backendRole.ifPresent(authorities::add);

        log.info("JWT subject: {}", jwt.getSubject());
        log.info("Backend role: {}", backendRole.orElse(null));
        log.info("Authorities: {}", authorities);
        return new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
    }

    @SuppressWarnings("unchecked")
    private Collection<GrantedAuthority> extractRoles(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess == null) {
            return List.of();
        }

        List<String> roles = (List<String>) realmAccess.get("roles");
        if (roles == null) {
            return List.of();
        }

        return roles.stream()
            .map(role -> new SimpleGrantedAuthority("KEYCLOAK_" + role))
            .map(GrantedAuthority.class::cast)
            .toList();
    }

    private Optional<GrantedAuthority> getBackendRoleAuthority(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());

        return userAccountRepository.findById(keycloakId)
                .map(UserAccountEntity::getRole)
                .map(role ->
                        new SimpleGrantedAuthority("ROLE_" + role.name()));

    }
}
