package de.thm.swtp.api.exceptionhandling;

import de.thm.swtp.api.exceptionhandling.exceptions.ProfileAccessDeniedException;
import de.thm.swtp.api.projectInvitation.exception.InvalidProjectInviteException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteAccessDeniedException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteNotFoundException;
import de.thm.swtp.api.tag.exception.TagAccessDeniedException;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.project.exception.*;
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

    @ExceptionHandler(ExceptionProjectResponse.class)
    public ResponseEntity<ErrorResponse> handleProjectAlreadyExists(ExceptionProjectResponse ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionOwnerNotFound.class)
    public ResponseEntity<ErrorResponse> handleOwnerNotFound(ExceptionOwnerNotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectNotFound.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(ExceptionProjectNotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectAlreadyDeleted.class)
    public ResponseEntity<ErrorResponse> handleProjectAlreadyDeleted(ExceptionProjectAlreadyDeleted ex) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ErrorResponse.of(410, "Gone", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectDeleteNotAllowed.class)
    public ResponseEntity<ErrorResponse> handleProjectDeleteNotAllowed(ExceptionProjectDeleteNotAllowed ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectNameAlreadyExists.class)
    public ResponseEntity<ErrorResponse> handleProjectNameAlreadyExists(ExceptionProjectNameAlreadyExists ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectEditNotAllowed.class)
    public ResponseEntity<ErrorResponse> handleProjectEditNotAllowed(ExceptionProjectEditNotAllowed ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ProjectInviteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectInviteNotFound(ProjectInviteNotFoundException ex) {
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