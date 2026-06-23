package de.thm.swtp.api.exceptionhandling;

import de.thm.swtp.api.exceptionhandling.exceptions.*;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestAlreadyExistsException;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestInvalidStatusException;
import de.thm.swtp.api.professorRequest.exception.ProfessorRequestNotFoundException;
import de.thm.swtp.api.projectFavorite.exception.ProjectAlreadyFavoritedException;
import de.thm.swtp.api.userFollow.exception.CannotFollowYourselfException;
import de.thm.swtp.api.userFollow.exception.UserAlreadyFollowingException;
import de.thm.swtp.api.userFollow.exception.UserFollowNotFoundException;
import de.thm.swtp.api.projectFavorite.exception.ProjectFavoriteNotFoundException;
import de.thm.swtp.api.projectInvitation.exception.InvalidProjectInviteException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteAccessDeniedException;
import de.thm.swtp.api.projectInvitation.exception.ProjectInviteNotFoundException;
import de.thm.swtp.api.tag.exception.TagAccessDeniedException;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.project.exception.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

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

    @ExceptionHandler(ProjectJoinRequestAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestAccessDenied(ProjectJoinRequestAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }
    @ExceptionHandler(ProjectJoinRequestAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestAlreadyExists(ProjectJoinRequestAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectJoinRequestInvalidStatusForEditException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestInvalidStatusForEdit(ProjectJoinRequestInvalidStatusForEditException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectJoinRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestNotFound(ProjectJoinRequestNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectLinkAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProjectLinkAlreadyExists(ProjectLinkAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectLinkDoesNotBelongToProjectException.class)
    public ResponseEntity<ErrorResponse> handleProjectLinkDoesNotBelongToProject(ProjectLinkDoesNotBelongToProjectException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectAlreadyFavoritedException.class)
    public ResponseEntity<ErrorResponse> handleProjectAlreadyFavorited(ProjectAlreadyFavoritedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFavoriteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectFavoriteNotFound(ProjectFavoriteNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectMemberNotFound(ProjectMemberNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectOwnerCannotBeRemovedException.class)
    public ResponseEntity<ErrorResponse> handleProjectOwnerCannotBeRemoved(ProjectOwnerCannotBeRemovedException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundByUrlException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFoundByUrl(ProjectNotFoundByUrlException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(InvalidProjectPostException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectPost(InvalidProjectPostException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ProjectPostAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProjectPostAccessDenied(ProjectPostAccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkAlreadyExists(UserProfileLinkAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkDoesNotBelongToProfileException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkDoesNotBelongToProfile(UserProfileLinkDoesNotBelongToProfileException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkEditNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkEditNotAllowed(UserProfileLinkEditNotAllowedException ex) {

        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ProjectPostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectPostNotFound(ProjectPostNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkNotFound(UserProfileLinkNotFoundException ex) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProfessorRequestAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProfessorRequestAlreadyExists(ProfessorRequestAlreadyExistsException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProfessorRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfessorRequestNotFound(ProfessorRequestNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProfessorRequestInvalidStatusException.class)
    public ResponseEntity<ErrorResponse> handleProfessorRequestInvalidStatus(ProfessorRequestInvalidStatusException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileNotFound(ProjectFileNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileDoesNotBelongToProjectException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileDoesNotBelongToProject(ProjectFileDoesNotBelongToProjectException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileTypeNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileTypeNotAllowed(ProjectFileTypeNotAllowedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(415, "Unsupported Media Type", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileUploadLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileUploadLimitExceeded(ProjectFileUploadLimitExceededException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Unprocessable Entity", ex.getMessage()));
    }

    @ExceptionHandler(CannotFollowYourselfException.class)
    public ResponseEntity<ErrorResponse> handleCannotFollowYourself(CannotFollowYourselfException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyFollowingException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyFollowing(UserAlreadyFollowingException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", message));
    }

    @ExceptionHandler(UserFollowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserFollowNotFound(UserFollowNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionInvalidProjectUrl.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectUrl(ExceptionInvalidProjectUrl ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectUrlAlreadyExists.class)
    public ResponseEntity<ErrorResponse> handleProjectUrlAlreadyExists(ExceptionProjectUrlAlreadyExists ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectUrlGenerationFailed.class)
    public ResponseEntity<ErrorResponse> handleProjectUrlGenerationFailed(ExceptionProjectUrlGenerationFailed ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", ex.getMessage()));
    }
}