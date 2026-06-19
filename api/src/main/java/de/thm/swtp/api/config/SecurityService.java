package de.thm.swtp.api.config;

import de.thm.swtp.api.project.ProjectRepository;
import de.thm.swtp.api.userprofile.domain.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component("security")
@RequiredArgsConstructor
public class SecurityService {

    private final ProjectRepository projectRepository;



    public boolean isAdmin(Authentication authentication) {
        return hasRole(authentication, UserRole.ADMIN);
    }

    public boolean isUser(Authentication authentication) {
        return hasRole(authentication, UserRole.USER);
    }

    public boolean isCurrentUser(UUID userId, Authentication authentication) {
        if (!hasUserContext(userId, authentication)) {
            return false;
        }
        return userId.equals(getCurrentUserId(authentication));
    }

    public boolean isProjectOwner(UUID projectId, Authentication authentication) {
        if (!hasProjectContext(projectId, authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return projectRepository.existsByIdAndMembersKeycloakId(projectId,currentUserId);
    }

    public boolean isProjectMember(UUID projectId, Authentication authentication) {
        if (!hasProjectContext(projectId, authentication)) {
            return false;
        }
        UUID currentUserId = getCurrentUserId(authentication);
        return projectRepository.existsByIdAndMembersKeycloakId(projectId,currentUserId);
    }

    public boolean isProjectContributer(UUID projectId, Authentication authentication) {
        return isProjectOwner(projectId, authentication) || isProjectMember(projectId, authentication);
    }


    private String getAuthority(UserRole role) {
        return "ROLE_" + role.name();
    }
    private UUID getCurrentUserId(Authentication authentication) {
        return UUID.fromString( authentication.getName());
    }

    private boolean hasRole(Authentication authentication, UserRole role) {
        if (authentication == null || role == null) {
            return false;
        }

        String authority =  getAuthority(role);

        return authentication.getAuthorities()
                .stream()
                .anyMatch(grantedAuthority -> Objects.equals(grantedAuthority.getAuthority(), authority));
    }

    private boolean hasProjectContext(UUID projectId, Authentication authentication) {
        return projectId != null && authentication != null;
    }

    private boolean hasUserContext(UUID userId, Authentication authentication) {
        return userId != null && authentication != null;
    }
}

