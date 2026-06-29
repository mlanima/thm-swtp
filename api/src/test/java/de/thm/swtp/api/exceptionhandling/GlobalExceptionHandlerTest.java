package de.thm.swtp.api.exceptionhandling;

import de.thm.swtp.api.exceptionhandling.exceptions.ProjectFileUploadLimitExceededException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectFileTypeNotAllowedException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectJoinRequestAccessDeniedException;
import de.thm.swtp.api.exceptionhandling.exceptions.ProjectMemberNotFoundException;
import de.thm.swtp.api.project.exception.ExceptionProjectAlreadyDeleted;
import de.thm.swtp.api.project.exception.ExceptionProjectNotFound;
import de.thm.swtp.api.project.exception.ExceptionProjectResponse;
import de.thm.swtp.api.project.exception.ExceptionProjectUrlGenerationFailed;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the {@link GlobalExceptionHandler} status matrix directly: each handler is a
 * plain method returning a ResponseEntity, so we call it and assert status + body.
 * Covers every status code the funnel emits, all framework handlers (so the catch-all
 * can't silently shadow them — the regression fixed in this branch) and the catch-all.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    private static void assertStatus(ResponseEntity<ErrorResponse> r, HttpStatus expected) {
        assertThat(r.getStatusCode()).isEqualTo(expected);
        assertThat(r.getBody()).isNotNull();
        assertThat(r.getBody().getStatusCode()).isEqualTo(expected.value());
    }

    // ── domain exceptions, one per status code ──

    @Test
    void accessDeniedDomainExceptionReturns403() {
        assertStatus(handler.handleProjectJoinRequestAccessDenied(
                new ProjectJoinRequestAccessDeniedException("nope")), HttpStatus.FORBIDDEN);
        assertThat(handler.handleProjectJoinRequestAccessDenied(
                new ProjectJoinRequestAccessDeniedException("nope")).getBody().getError()).isEqualTo("Forbidden");
    }

    @Test
    void projectNotFoundReturns404() {
        assertStatus(handler.handleProjectNotFound(
                new ExceptionProjectNotFound(UUID.randomUUID())), HttpStatus.NOT_FOUND);
    }

    @Test
    void projectAlreadyExistsReturns409() {
        assertStatus(handler.handleProjectAlreadyExists(
                new ExceptionProjectResponse("slug")), HttpStatus.CONFLICT);
    }

    @Test
    void projectAlreadyDeletedReturns410() {
        assertStatus(handler.handleProjectAlreadyDeleted(
                new ExceptionProjectAlreadyDeleted(UUID.randomUUID())), HttpStatus.GONE);
    }

    @Test
    void fileTypeNotAllowedReturns415() {
        assertStatus(handler.handleProjectFileTypeNotAllowed(
                new ProjectFileTypeNotAllowedException("evil.exe")), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void fileUploadLimitReturns422() {
        assertStatus(handler.handleProjectFileUploadLimitExceeded(
                new ProjectFileUploadLimitExceededException(20)), HttpStatus.valueOf(422));
    }

    @Test
    void memberNotFoundReturns404() {
        assertStatus(handler.handleProjectMemberNotFound(
                new ProjectMemberNotFoundException(UUID.randomUUID(), UUID.randomUUID())), HttpStatus.NOT_FOUND);
    }

    // ── technical 500 with error log ──

    @Test
    void urlGenerationFailedReturns500() {
        assertStatus(handler.handleProjectUrlGenerationFailed(
                new ExceptionProjectUrlGenerationFailed("slug")), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ── framework exceptions: explicit handlers must beat the catch-all ──

    @Test
    void springAccessDeniedReturns403WithGenericPermissionBody() {
        // 403 must be uniform with RestAccessDeniedHandler (URL-based denials): same generic
        // body, never ex.getMessage(). The handler still logs the real message at warn for audit.
        ResponseEntity<ErrorResponse> r = handler.handleAccessDeniedSpring(new AccessDeniedException("denied"));
        assertStatus(r, HttpStatus.FORBIDDEN);
        assertThat(r.getBody().getMessage()).isEqualTo("You do not have permission to perform this action.");
        assertThat(r.getBody().getMessage()).doesNotContain("denied");
    }

    @Test
    void validationReturns400() {
        // Handler joins field-error messages from the BindingResult, so build a real one
        // rather than mocking the exception (a mock's getBindingResult() returns null).
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "obj");
        bindingResult.addError(new FieldError("obj", "name", "must not be blank"));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);
        assertStatus(handler.handleValidation(ex), HttpStatus.BAD_REQUEST);
    }

    @Test
    void unreadableBodyReturns400() {
        assertStatus(handler.handleUnreadable(new org.springframework.http.converter.HttpMessageNotReadableException(
                "bad", (org.springframework.http.HttpInputMessage) null)), HttpStatus.BAD_REQUEST);
    }

    @Test
    void missingParamReturns400() {
        assertStatus(handler.handleMissingParam(
                new MissingServletRequestParameterException("q", "String")), HttpStatus.BAD_REQUEST);
    }

    @Test
    void methodNotSupportedReturns405() {
        assertStatus(handler.handleMethodNotSupported(
                new HttpRequestMethodNotSupportedException("TRACE")), HttpStatus.METHOD_NOT_ALLOWED);
    }

    @Test
    void mediaTypeNotSupportedReturns415() {
        assertStatus(handler.handleMediaTypeNotSupported(
                new HttpMediaTypeNotSupportedException("nope")), HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @Test
    void uploadTooLargeReturns413() {
        assertStatus(handler.handleUploadTooLarge(
                new MaxUploadSizeExceededException(1024L)), HttpStatus.valueOf(413));
    }

    // ── catch-all: 500 with a generic body, never the raw exception message ──

    @Test
    void unexpectedExceptionReturns500WithGenericBody() {
        ResponseEntity<ErrorResponse> r = handler.handleUnexpected(new RuntimeException("DB connection leaked"));
        assertStatus(r, HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(r.getBody().getMessage()).isEqualTo("An unexpected error occurred.");
        assertThat(r.getBody().getMessage()).doesNotContain("DB connection leaked");
    }
}
