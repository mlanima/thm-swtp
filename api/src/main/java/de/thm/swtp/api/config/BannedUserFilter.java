package de.thm.swtp.api.config;

import de.thm.swtp.api.userprofile.domain.UserStatus;
import de.thm.swtp.api.userprofile.repository.UserProfileRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BannedUserFilter extends OncePerRequestFilter {

    private final UserProfileRepository userProfileRepository;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();

        return path.equals("/api/v1/users/me/ban-status")
                || path.startsWith("/api/public/")
                || path.equals("/actuator/health")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/v3/api-docs/");
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            UUID keycloakId = UUID.fromString(jwtAuth.getToken().getSubject());

            boolean banned = userProfileRepository.existsByKeycloakIdAndStatus(
                    keycloakId,
                    UserStatus.BANNED
            );

            if (banned) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Account is banned");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
