package gov.kh.moi.inb.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler} - every endpoint in this service relies on it to
 * translate exceptions into the standard {@link ApiError} shape, but it previously had no direct
 * tests of its own; most branches were only ever reached incidentally through a handful of
 * integration tests for unrelated endpoints. Exercises each handler method directly so the
 * status code, message, and (where relevant) violations list are pinned down independently of
 * whatever controller happens to trigger them.
 */
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/api/admin/articles/1");
    }

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ApiError> response = handler.handleNotFound(
                ResourceNotFoundException.of("Article", 1L), request);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().getMessage()).isEqualTo("Article not found with identifier: 1");
    }

    @Test
    void handleDuplicate_returns409() {
        ResponseEntity<ApiError> response = handler.handleDuplicate(
                new DuplicateResourceException("Slug already exists"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(response.getBody().getMessage()).isEqualTo("Slug already exists");
    }

    @Test
    void handleDataIntegrity_returns409_withGenericMessage() {
        ResponseEntity<ApiError> response = handler.handleDataIntegrity(
                new DataIntegrityViolationException("duplicate key value violates unique constraint"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        // Deliberately generic - must never leak the raw DB constraint name to the client.
        assertThat(response.getBody().getMessage()).contains("conflicts with existing data");
    }

    @Test
    void handleValidation_returns400_withFieldViolations() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "articleRequestDTO");
        bindingResult.addError(new FieldError("articleRequestDTO", "title", "Title is required"));

        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getBindingResult()).thenReturn(bindingResult);

        ResponseEntity<ApiError> response = handler.handleValidation(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getViolations()).hasSize(1);
        assertThat(response.getBody().getViolations().get(0).getField()).isEqualTo("title");
        assertThat(response.getBody().getViolations().get(0).getMessage()).isEqualTo("Title is required");
    }

    @Test
    void handleMalformedJson_returns400() {
        ResponseEntity<ApiError> response = handler.handleMalformedJson(
                new HttpMessageNotReadableException("JSON parse error"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("Malformed JSON in request body.");
    }

    @Test
    void handleMaxUpload_returns413() {
        ResponseEntity<ApiError> response = handler.handleMaxUpload(
                new MaxUploadSizeExceededException(10 * 1024 * 1024L), request);

        assertThat(response.getStatusCode().value()).isEqualTo(413);
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<ApiError> response = handler.handleAccessDenied(
                new AccessDeniedException("denied"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(403);
        assertThat(response.getBody().getMessage()).isEqualTo("You do not have permission to perform this action.");
    }

    @Test
    void handleBadCredentials_returns401() {
        ResponseEntity<ApiError> response = handler.handleBadCredentials(
                new BadCredentialsException("bad creds"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(401);
        assertThat(response.getBody().getMessage()).isEqualTo("Invalid credentials.");
    }

    @Test
    void handlePropertyReference_returns400_namingTheBadProperty() {
        PropertyReferenceException ex = mock(PropertyReferenceException.class);
        when(ex.getPropertyName()).thenReturn("bogusField");

        ResponseEntity<ApiError> response = handler.handlePropertyReference(ex, request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).contains("bogusField");
    }

    @Test
    void handleIllegalArgument_returns400_withExceptionMessage() {
        ResponseEntity<ApiError> response = handler.handleIllegalArgument(
                new IllegalArgumentException("A category cannot be its own parent"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody().getMessage()).isEqualTo("A category cannot be its own parent");
    }

    @Test
    void handleGeneric_returns500_withoutLeakingInternalDetails() {
        ResponseEntity<ApiError> response = handler.handleGeneric(
                new RuntimeException("some internal stack trace detail"), request);

        assertThat(response.getStatusCode().value()).isEqualTo(500);
        assertThat(response.getBody().getMessage()).doesNotContain("some internal stack trace detail");
    }
}
