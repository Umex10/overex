package com.overex.backend.common.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * Centralised translation of thrown exceptions into HTTP error responses.
 *
 * <p>
 * Maps domain and framework exceptions to the appropriate status codes so
 * controllers stay free of error-handling boilerplate. Every handler answers
 * with the same {@link ApiError} body, built by {@link #build}: the frontend
 * only has to know one error shape.
 *
 * @see OwnException
 * @see ApiError
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	/**
	 * Translates {@link OwnException} (our business-rule violations, e.g.
	 * duplicate email/phone) into a 400 response with field-level details.
	 *
	 * @return a 400 response carrying one field error per broken rule
	 */
	@ExceptionHandler(OwnException.class)
	public ResponseEntity<ApiError> handleOwnException(OwnException ex, HttpServletRequest request) {
		return build(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getErrors(), request);
	}

	/**
	 * Translates Jakarta Bean Validation failures (e.g. {@code @Email},
	 * {@code @Size} on the request DTOs) into the same shape as
	 * {@link OwnException}, so the frontend can replay every field error the same
	 * way regardless of where it originated.
	 *
	 * @return a 400 response carrying one field error per invalid field
	 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<ApiError.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> ApiError.FieldError.builder()
						.field(fe.getField())
						.message(fe.getDefaultMessage())
						.build())
				.toList();

		return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors, request);
	}

	/**
	 * Translates a path or query parameter of the wrong type (e.g. {@code abc}
	 * where a UUID is expected) into a 400 with one field error for that
	 * parameter. Without this handler the catch-all below would answer with a
	 * misleading 500, although the client sent a bad request.
	 *
	 * @return a 400 response carrying one field error for the parameter
	 */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiError> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest request) {
		List<ApiError.FieldError> fieldErrors = List.of(ApiError.FieldError.builder()
				.field(ex.getName())
				.message("The value '" + ex.getValue() + "' has the wrong format.")
				.build());

		return build(HttpStatus.BAD_REQUEST, "Validation failed", fieldErrors, request);
	}

	/**
	 * Maps Spring Security {@link AuthenticationException}s to a generic 401
	 * response so clients cannot enumerate valid emails through the error
	 * message.
	 *
	 * @return a 401 response with a generic credential-failure message
	 */
	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiError> handleAuthenticationException(AuthenticationException ex,
			HttpServletRequest request) {
		return build(HttpStatus.UNAUTHORIZED, "The credentials are incorrect.", List.of(), request);
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
	public ResponseEntity<ApiError> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		return build(HttpStatus.FORBIDDEN, "You do not have permission to access this resource.", List.of(),
				request);
	}

	/**
	 * Translates {@link EntityNotFoundException} into a 404. Its message names
	 * the missing entity (e.g. "Event '…' was not found") and is safe to show.
	 *
	 * @return a 404 response carrying the not-found message
	 */
	@ExceptionHandler(EntityNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(EntityNotFoundException ex, HttpServletRequest request) {
		return build(HttpStatus.NOT_FOUND, ex.getMessage(), List.of(), request);
	}

	/**
	 * Translates a lost-update conflict into a 409. Thrown by hand when the
	 * client's version is outdated, and by Hibernate when another transaction
	 * saved in the moment between loading and saving.
	 *
	 * <p>
	 * The exception's own message is technical, so a fixed one is sent instead.
	 *
	 * @return a 409 response asking the client to reload
	 */
	@ExceptionHandler(ObjectOptimisticLockingFailureException.class)
	public ResponseEntity<ApiError> handleOptimisticLock(ObjectOptimisticLockingFailureException ex,
			HttpServletRequest request) {
		return build(HttpStatus.CONFLICT, "This resource was changed in the meantime. Reload it and try again.",
				List.of(), request);
	}

	/**
	 * Catch-all handler that maps any unhandled exception to a 500 response.
	 *
	 * <p>
	 * The real message can contain internals (SQL, class names), so it only goes
	 * to the log; the client gets a fixed text.
	 *
	 * @return a 500 response with a generic message
	 */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleGeneric(Exception ex, HttpServletRequest request) {
		log.error("Unhandled exception on {}", request.getRequestURI(), ex);
		return build(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again later.", List.of(),
				request);
	}

	/**
	 * Builds the response every handler above returns: an {@link ApiError} with
	 * all four fields set, wrapped in a {@link ResponseEntity} with the matching
	 * HTTP status.
	 *
	 * @param errors the rejected fields, or {@code List.of()} when no single
	 *               field is to blame
	 * @return the response with status code and error body
	 */
	private ResponseEntity<ApiError> build(HttpStatus status, String message, List<ApiError.FieldError> errors,
			HttpServletRequest request) {
		ApiError error = ApiError.builder()
				.status(status.value())
				.message(message)
				.path(request.getRequestURI())
				.errors(errors)
				.build();
		return new ResponseEntity<>(error, status);
	}

}
