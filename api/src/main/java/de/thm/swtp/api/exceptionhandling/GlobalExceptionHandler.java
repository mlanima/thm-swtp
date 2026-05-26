package de.thm.swtp.api.exceptionhandling;

import de.thm.swtp.api.exceptionhandling.exceptions.ProfileAccessDeniedException;
import de.thm.swtp.api.project.exception.ProjectNotFoundException;
import de.thm.swtp.api.projectInvitation.exception.InvalidProjectInviteException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteAccessDeniedException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteNotFoundException;
import de.thm.swtp.api.tag.exception.TagAccessDeniedException;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProfileAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(ProfileAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileNotFound(UserProfileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(ProjectNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectInviteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(ProjectInviteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectInviteAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProjectInviteAccessDenied(ProjectInviteAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(InvalidProjectInviteException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectInvite(InvalidProjectInviteException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }


    @ExceptionHandler(TagAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTagAccessDenied(TagAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }
}
