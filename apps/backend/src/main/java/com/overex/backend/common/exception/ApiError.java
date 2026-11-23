package com.overex.backend.common.exception;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The one error body the backend sends for every failed request (400 to 500).
 *
 * <p>Every field is always set, so the frontend can read each error the same
 * way: {@code status} and {@code message} describe the error, {@code path} is
 * the requested URI, and {@code errors} lists the rejected fields. When no
 * single field is to blame (e.g. 404, 409, 500), {@code errors} is an empty
 * list, never {@code null}.
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
	private String path;
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
