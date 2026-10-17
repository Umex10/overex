package com.overex.backend.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.overex.backend.common.domain.dtos.ErrorResponse;
import com.overex.backend.common.domain.dtos.factory.ErrorResponseFactory;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.NonNull;

/**
 * Centralised translation of thrown exceptions into HTTP error responses.
 *
 * <p>
 * Maps domain and framework exceptions to the appropriate status codes and
 * response shapes so controllers stay free of error-handling boilerplate. Two
 * envelope shapes coexist: {@link ErrorResponse} (used for generic 404/500
 * errors) and {@link ApiError} (used for validation and authentication errors
 * where per-field details matter).
 *
 * @see OwnException
 * @see ApiError
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Translates {@link OwnException} (our business-rule violations, e.g.
	 * duplicate email/phone) into a 400 response with field-level details.
	 *
	 * @return a 400 response carrying the validation {@link ApiError}
	 */
	@ExceptionHandler(OwnException.class)
	public ResponseEntity<ApiError> handleOwnException(OwnException ex) {
		ApiError error = ApiError.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.message(ex.getMessage())
				.errors(ex.getErrors())
				.build();
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Translates Jakarta Bean Validation failures (e.g. {@code @Email},
	 * {@code @Size} on the request DTOs) into the same {@link ApiError} shape as
	 * {@link OwnException}, so the frontend can replay every field error the same
	 * way regardless of where it originated.
	 *
	 * @return a 400 response carrying one field error per invalid field
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> ApiError.FieldError.builder()
						.field(fe.getField())
						.message(fe.getDefaultMessage())
						.build())
				.toList();

		ApiError error = ApiError.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.message("Validation failed")
				.errors(fieldErrors)
				.build();
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Translates a path or query parameter of the wrong type (e.g. {@code abc}
	 * where a UUID is expected) into a 400 with one field error for that
	 * parameter. Without this handler the catch-all below would answer with a
	 * misleading 500, although the client sent a bad request.
	 *
	 * @return a 400 response carrying the validation {@link ApiError}
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
		ApiError error = ApiError.builder()
				.status(HttpStatus.BAD_REQUEST.value())
				.message("Validation failed")
				.errors(List.of(ApiError.FieldError.builder()
						.field(ex.getName())
						.message("The value '" + ex.getValue() + "' has the wrong format.")
						.build()))
				.build();
		return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
	}

	/**
	 * Maps Spring Security {@link AuthenticationException}s to a generic 401
	 * response so clients cannot enumerate valid emails through the error
	 * message.
	 *
	 * @return a 401 response with a generic credential-failure message
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex) {
		ApiError error = ApiError.builder()
				.status(HttpStatus.UNAUTHORIZED.value())
				.message("The credentials are incorrect.")
				.build();
		return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
	}

	/**
	 * Maps an {@link AccessDeniedException} (e.g. a failed
	 * {@code @PreAuthorize("hasRole('ADMIN')")} check raised during the
	 * controller invocation) to a clean 403 response.
	 *
	 * <p>
	 * Without this handler the catch-all below would turn role-based denials
	 * into a misleading 500.
	 *
	 * @return a 403 response with a generic permission-denied message
	 */
	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex) {
		ApiError error = ApiError.builder()
				.status(HttpStatus.FORBIDDEN.value())
				.message("You do not have permission to access this resource.")
				.build();
		return new ResponseEntity<>(error, HttpStatus.FORBIDDEN);
	}

	/**
	 * Translates {@link EntityNotFoundException} into a 404 response with the
	 * standard {@link ErrorResponse} envelope.
	 *
	 * @return a 404 response carrying the not-found error body
	 */
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<@NonNull ErrorResponse> handleNotFound(EntityNotFoundException ex,
			HttpServletRequest request) {
		return ResponseEntity.status(404).body(
				ErrorResponseFactory.notFound(ex.getMessage(), request.getRequestURI()));
	}
	
	/**
	 * Catch-all handler that maps any unhandled exception to a 500 response.
	 *
	 * @return a 500 response carrying the generic error envelope
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<@NonNull ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
		return ResponseEntity.status(500).body(
				ErrorResponseFactory.internalServerError(ex.getMessage(), request.getRequestURI()));
	}

}
