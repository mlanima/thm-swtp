package de.thm.swtp.api.exceptionhandling;

import de.thm.swtp.api.common.LogSafe;
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
import de.thm.swtp.api.tag.exception.TagNotValidException;
import de.thm.swtp.api.tag.validation.TagValidationException;
import de.thm.swtp.api.userprofile.exception.UserProfileNotFoundException;
import de.thm.swtp.api.project.exception.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ex.getMessage() is logged unstripped except for messages embedding raw free-text
    // request input (project name, project URL, upload Content-Type, JSON-deserialization
    // errors — Jackson surfaces decoded client values incl. CRLF) — those sanitize via
    // LogSafe.clean. Everything else is UUID/enums/validation/server-defined (no CRLF).
    // Free text that never reaches a handler message (search queries, upload filenames)
    // is sanitized at source.

    @ExceptionHandler(ProfileAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(ProfileAccessDeniedException ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileNotFound(UserProfileNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectResponse.class)
    public ResponseEntity<ErrorResponse> handleProjectAlreadyExists(ExceptionProjectResponse ex) {
        log.debug("Conflict (409): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionOwnerNotFound.class)
    public ResponseEntity<ErrorResponse> handleOwnerNotFound(ExceptionOwnerNotFound ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectNotFound.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFound(ExceptionProjectNotFound ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectAlreadyDeleted.class)
    public ResponseEntity<ErrorResponse> handleProjectAlreadyDeleted(ExceptionProjectAlreadyDeleted ex) {
        log.debug("Gone (410): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ErrorResponse.of(410, "Gone", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectDeleteNotAllowed.class)
    public ResponseEntity<ErrorResponse> handleProjectDeleteNotAllowed(ExceptionProjectDeleteNotAllowed ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectNameAlreadyExists.class)
    public ResponseEntity<ErrorResponse> handleProjectNameAlreadyExists(ExceptionProjectNameAlreadyExists ex) {
        log.debug("Conflict (409): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectEditNotAllowed.class)
    public ResponseEntity<ErrorResponse> handleProjectEditNotAllowed(ExceptionProjectEditNotAllowed ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ProjectInviteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectInviteNotFound(ProjectInviteNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectInviteAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProjectInviteAccessDenied(ProjectInviteAccessDeniedException ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(InvalidProjectInviteException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectInvite(InvalidProjectInviteException ex) {
        log.debug("Bad Request (400): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(TagAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTagAccessDenied(TagAccessDeniedException ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(TagNotValidException.class)
    public ResponseEntity<ErrorResponse> handleTagNotValid(TagNotValidException ex) {
        log.debug("Bad Request (400): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage(), "TAG_NOT_VALID"));
    }

    @ExceptionHandler(TagValidationException.class)
    public ResponseEntity<ErrorResponse> handleTagValidationError(TagValidationException ex) {
        log.error("Tag validation failed due to external API error: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(502, "Bad Gateway", "Tag validation service temporarily unavailable."));
    }

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ErrorResponse> handleResourceAccess(ResourceAccessException ex) {
        log.error("External API unreachable: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponse.of(502, "Bad Gateway", "Tag validation service temporarily unavailable."));
    }

    @ExceptionHandler(ProjectJoinRequestAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestAccessDenied(ProjectJoinRequestAccessDeniedException ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }
    @ExceptionHandler(ProjectJoinRequestAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestAlreadyExists(ProjectJoinRequestAlreadyExistsException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectJoinRequestInvalidStatusForEditException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestInvalidStatusForEdit(ProjectJoinRequestInvalidStatusForEditException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectJoinRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectJoinRequestNotFound(ProjectJoinRequestNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectLinkAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProjectLinkAlreadyExists(ProjectLinkAlreadyExistsException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectLinkDoesNotBelongToProjectException.class)
    public ResponseEntity<ErrorResponse> handleProjectLinkDoesNotBelongToProject(ProjectLinkDoesNotBelongToProjectException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectAlreadyFavoritedException.class)
    public ResponseEntity<ErrorResponse> handleProjectAlreadyFavorited(ProjectAlreadyFavoritedException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFavoriteNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectFavoriteNotFound(ProjectFavoriteNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectMemberNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectMemberNotFound(ProjectMemberNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectOwnerCannotBeRemovedException.class)
    public ResponseEntity<ErrorResponse> handleProjectOwnerCannotBeRemoved(ProjectOwnerCannotBeRemovedException ex) {
        log.debug("Bad Request (400): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ProjectNotFoundByUrlException.class)
    public ResponseEntity<ErrorResponse> handleProjectNotFoundByUrl(ProjectNotFoundByUrlException ex) {
        log.debug("Not Found (404): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(InvalidProjectPostException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectPost(InvalidProjectPostException ex) {
        log.debug("Bad Request (400): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ProjectPostAccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleProjectPostAccessDenied(ProjectPostAccessDeniedException ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkAlreadyExists(UserProfileLinkAlreadyExistsException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkDoesNotBelongToProfileException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkDoesNotBelongToProfile(UserProfileLinkDoesNotBelongToProfileException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkEditNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkEditNotAllowed(UserProfileLinkEditNotAllowedException ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", ex.getMessage()));
    }

    @ExceptionHandler(ProjectPostNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectPostNotFound(ProjectPostNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(UserProfileLinkNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserProfileLinkNotFound(UserProfileLinkNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProfessorRequestAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleProfessorRequestAlreadyExists(ProfessorRequestAlreadyExistsException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProfessorRequestNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProfessorRequestNotFound(ProfessorRequestNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProfessorRequestInvalidStatusException.class)
    public ResponseEntity<ErrorResponse> handleProfessorRequestInvalidStatus(ProfessorRequestInvalidStatusException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileNotFound(ProjectFileNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileDoesNotBelongToProjectException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileDoesNotBelongToProject(ProjectFileDoesNotBelongToProjectException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileTypeNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileTypeNotAllowed(ProjectFileTypeNotAllowedException ex) {
        log.warn("Unsupported Media Type (415): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(415, "Unsupported Media Type", ex.getMessage()));
    }

    @ExceptionHandler(ProjectFileUploadLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleProjectFileUploadLimitExceeded(ProjectFileUploadLimitExceededException ex) {
        log.warn("Unprocessable Entity (422): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.valueOf(422))
                .body(ErrorResponse.of(422, "Unprocessable Entity", ex.getMessage()));
    }

    @ExceptionHandler(CannotFollowYourselfException.class)
    public ResponseEntity<ErrorResponse> handleCannotFollowYourself(CannotFollowYourselfException ex) {
        log.debug("Bad Request (400): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(UserAlreadyFollowingException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyFollowing(UserAlreadyFollowingException ex) {
        log.debug("Conflict (409): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(UserFollowNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserFollowNotFound(UserFollowNotFoundException ex) {
        log.debug("Not Found (404): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionInvalidProjectUrl.class)
    public ResponseEntity<ErrorResponse> handleInvalidProjectUrl(ExceptionInvalidProjectUrl ex) {
        log.debug("Bad Request (400): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectUrlAlreadyExists.class)
    public ResponseEntity<ErrorResponse> handleProjectUrlAlreadyExists(ExceptionProjectUrlAlreadyExists ex) {
        log.debug("Conflict (409): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    @ExceptionHandler(ExceptionProjectUrlGenerationFailed.class)
    public ResponseEntity<ErrorResponse> handleProjectUrlGenerationFailed(ExceptionProjectUrlGenerationFailed ex) {
        log.error("Project URL generation failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", ex.getMessage()));
    }

    @ExceptionHandler(ProjectOwnerTransferToSelfException.class)
    public ResponseEntity<ErrorResponse> handleProjectOwnerTransferToSelf(ProjectOwnerTransferToSelfException ex) {
        log.warn("Unprocessable Entity (422): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Unprocessable Entity", ex.getMessage()));
    }

    @ExceptionHandler(ProjectOwnerTransferToNonMemberException.class)
    public ResponseEntity<ErrorResponse> handleProjectOwnerTransferToNonMember(ProjectOwnerTransferToNonMemberException ex) {
        log.warn("Unprocessable Entity (422): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of(422, "Unprocessable Entity", ex.getMessage()));
    }

    // ── Framework exceptions: explicit handlers so the catch-all doesn't shadow them ─

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedSpring(AccessDeniedException ex) {
        log.warn("Forbidden (403): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", "You do not have permission to perform this action."));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.debug("Bad Request (400): {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("Bad Request (400): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(MissingServletRequestParameterException ex) {
        log.debug("Bad Request (400): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.debug("Method Not Allowed (405): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResponse.of(405, "Method Not Allowed", ex.getMessage()));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("Unsupported Media Type (415): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResponse.of(415, "Unsupported Media Type", ex.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        log.warn("Payload Too Large (413): {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.valueOf(413))
                .body(ErrorResponse.of(413, "Payload Too Large", ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception: type={}, message={}", ex.getClass().getName(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred."));
    }

    @ExceptionHandler(InvalidUserManagementSortFieldException.class)
    public ResponseEntity<ErrorResponse> handleInvalidUserManagementSortField(InvalidUserManagementSortFieldException ex) {
        log.debug("Bad Request (400): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler(InvalidProfessorEmailDomainException.class)
    public ResponseEntity<ErrorResponse> handleInvalidProfessorEmailDomain(InvalidProfessorEmailDomainException ex) {
        log.debug("Bad Request (400): {}", LogSafe.clean(ex.getMessage()));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }
}
