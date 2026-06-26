package de.thm.swtp.api.thesis;

import de.thm.swtp.api.thesis.dto.request.CreateThesisRequest;
import de.thm.swtp.api.thesis.dto.request.UpdateThesisRequest;
import de.thm.swtp.api.thesis.dto.response.DeleteThesisResponse;
import de.thm.swtp.api.thesis.dto.response.ThesisResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/theses")
@RequiredArgsConstructor
public class ThesisController {

    private final ThesisService thesisService;

    @GetMapping("/{thesisId}")
    @PreAuthorize("@security.canViewThesis(#thesisId, authentication)")
    public ResponseEntity<ThesisResponse> getById(@PathVariable UUID thesisId) {
        return ResponseEntity.ok(ThesisResponse.toResponse(thesisService.getById(thesisId)));
    }

    @GetMapping("/by-url/{thesisUrl}")
    @PreAuthorize("@security.canViewThesisByUrl(#thesisUrl, authentication)")
    public ResponseEntity<ThesisResponse> getByUrl(@PathVariable String thesisUrl) {
        return ResponseEntity.ok(ThesisResponse.toResponse(thesisService.getByUrl(thesisUrl)));
    }

    @PostMapping
    @PreAuthorize("@security.canCreateThesis(authentication)")
    public ResponseEntity<ThesisResponse> create(
            @Valid @RequestBody CreateThesisRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        UUID currentUserId = UUID.fromString(jwt.getSubject());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ThesisResponse.toResponse(thesisService.create(request, currentUserId)));
    }

    @PutMapping("/{thesisId}")
    @PreAuthorize("@security.canEditThesis(#thesisId, authentication)")
    public ResponseEntity<ThesisResponse> update(
            @PathVariable UUID thesisId,
            @Valid @RequestBody UpdateThesisRequest request) {
        return ResponseEntity.ok(ThesisResponse.toResponse(thesisService.update(thesisId, request)));
    }

    @DeleteMapping("/{thesisId}")
    @PreAuthorize("@security.canDeleteThesis(#thesisId, authentication)")
    public ResponseEntity<DeleteThesisResponse> delete(@PathVariable UUID thesisId) {
        return ResponseEntity.ok(thesisService.delete(thesisId));
    }

    @PostMapping("/{thesisId}/students/{studentKeycloakId}")
    @PreAuthorize("@security.canManageThesisStudents(#thesisId, authentication)")
    public ResponseEntity<ThesisResponse> addStudent(
            @PathVariable UUID thesisId,
            @PathVariable UUID studentKeycloakId) {
        return ResponseEntity.ok(ThesisResponse.toResponse(thesisService.addStudent(thesisId, studentKeycloakId)));
    }

    @DeleteMapping("/{thesisId}/students/{studentKeycloakId}")
    @PreAuthorize("@security.canManageThesisStudents(#thesisId, authentication)")
    public ResponseEntity<ThesisResponse> removeStudent(
            @PathVariable UUID thesisId,
            @PathVariable UUID studentKeycloakId) {
        return ResponseEntity.ok(ThesisResponse.toResponse(thesisService.removeStudent(thesisId, studentKeycloakId)));
    }

    @GetMapping("/url-exists/{thesisUrl}")
    public ResponseEntity<Boolean> thesisUrlExists(@PathVariable String thesisUrl) {
        return ResponseEntity.ok(thesisService.thesisUrlExists(thesisUrl));
    }
}
