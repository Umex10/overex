package com.overex.backend.common.exception;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Structured error envelope returned for validation and business-rule failures.
 *
 * <p>Contains the HTTP status code, a human-readable summary message and the
 * per-field validation errors that the frontend can map onto individual form
 * inputs.
 *
 * @see OwnException
 * @see GlobalExceptionHandler
 */
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApiError {

	private int status;
	private String message;
	private List<FieldError> errors;

	/**
	 * Single field-level validation error.
	 *
	 * <p>Used by the sign-up flow to report problems such as
	 * {@code "email already taken"} so the web client can attach the message to
	 * the right input.
	 */
	@Builder
	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class FieldError {
		private String field;
		private String message;
	}

}
