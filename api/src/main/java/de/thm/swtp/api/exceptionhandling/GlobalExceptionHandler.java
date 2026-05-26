package de.thm.swtp.api.exceptionhandling;

import de.thm.swtp.api.exceptionhandling.exceptions.ProfileAccessDeniedException;
import de.thm.swtp.api.project.exception.ExceptionProjectResponse;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.project.exception.ExceptionOwnerNotFound;
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
                .body(ErrorResponse.of(409,"Conflict",ex.getMessage()));
    }

    @ExceptionHandler(ExceptionOwnerNotFound.class)
    public ResponseEntity<ErrorResponse> handleOwnerNotFound(ExceptionOwnerNotFound ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }
}
