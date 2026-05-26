package de.thm.swtp.api.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class HelloController {

    @GetMapping("/public/hello")
    public Map<String, String> publicHello() {
        return Map.of("message", "Hello, World!"); // random comment...
    }

    @GetMapping("/hello")
    public Map<String, Object> securedHello(@AuthenticationPrincipal Jwt jwt, Authentication authentication) {
        return Map.of(
            "message", "Hello, " + jwt.getClaimAsString("preferred_username") + "!",
            "userId", jwt.getSubject(),
            "roles", authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList()
        );
    }
}

