package com.overex.backend.common.exception;

import java.util.List;

import lombok.Getter;

/**
 * Domain exception used to signal business-rule violations such as a duplicated
 * email or phone number during sign-up.
 *
 * <p>The carried list of {@link ApiError.FieldError} instances lets the global
 * exception handler render an {@link ApiError} with HTTP 400 so the client can
 * highlight the offending form fields.
 *
 * @see GlobalExceptionHandler#handleOwnException(OwnException)
 */
@Getter
public class OwnException extends RuntimeException {

	private final List<ApiError.FieldError> errors;

	/**
	 * Creates a new exception that wraps one or more field-level errors.
	 *
	 * <p>The message is auto-derived from the list size so log statements include
	 * a meaningful summary without callers having to build one.
	 */
	public OwnException(List<ApiError.FieldError> errors) {
		super("There " + (errors.size() == 1 ? "is " : "are ") + errors.size()
				+ (errors.size() == 1 ? " error" : " errors"));
		this.errors = errors;
	}

}
