package de.thm.swtp.api.github.controller;

import de.thm.swtp.api.github.config.GithubOAuthProperties;
import de.thm.swtp.api.github.dto.GithubAuthorizeUrlResponse;
import de.thm.swtp.api.github.dto.GithubCallbackRequest;
import de.thm.swtp.api.github.dto.GithubConnectionStatusResponse;
import de.thm.swtp.api.github.service.GithubConnectionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/github/connection")
public class GithubConnectionController {

    private final GithubConnectionService githubConnectionService;
    private final GithubOAuthProperties properties;

    @GetMapping
    @PreAuthorize("@security.canManageGithubConnection(authentication)")
    public GithubConnectionStatusResponse getStatus(@AuthenticationPrincipal Jwt jwt) {
        var connection = githubConnectionService.getConnection(currentUserId(jwt)).orElse(null);
        return GithubConnectionStatusResponse.toResponse(properties.enabled(), connection);
    }

    @PostMapping("/authorize-url")
    @PreAuthorize("@security.canManageGithubConnection(authentication)")
    public GithubAuthorizeUrlResponse getAuthorizeUrl(@AuthenticationPrincipal Jwt jwt) {
        return new GithubAuthorizeUrlResponse(githubConnectionService.buildAuthorizeUrl(currentUserId(jwt)));
    }

    @PostMapping("/callback")
    @PreAuthorize("@security.canManageGithubConnection(authentication)")
    public GithubConnectionStatusResponse completeCallback(
            @Valid @RequestBody GithubCallbackRequest request, @AuthenticationPrincipal Jwt jwt) {
        var connection = githubConnectionService.completeConnection(currentUserId(jwt), request.code(), request.state());
        return GithubConnectionStatusResponse.toResponse(properties.enabled(), connection);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("@security.canManageGithubConnection(authentication)")
    public void disconnect(@AuthenticationPrincipal Jwt jwt) {
        githubConnectionService.disconnect(currentUserId(jwt));
    }

    private UUID currentUserId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
