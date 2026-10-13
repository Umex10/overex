package com.overex.backend.common.domain.dtos;

import lombok.Getter;

/**
 * Standard error envelope returned for generic failures such as 404 and 500.
 *
 * <p>Mirrors the shape used by typical Spring Boot apps so clients can rely on a
 * predictable set of fields: HTTP status code, the canonical reason phrase, the
 * underlying message and the request path that triggered the error.
 *
 * @see com.overex.backend.common.domain.dtos.factory.ErrorResponseFactory
 * @see com.overex.backend.common.exception.GlobalExceptionHandler
 */
@Getter
public class ErrorResponse {

	private final int status;
	private final String error;
	private final String message;
	private final String path;

	/**
	 * Builds an error response with the supplied attributes. When {@code error}
	 * is {@code null} it defaults to {@code "Internal Server Error"}.
	 */
	public ErrorResponse(int status, String error, String message, String path) {
		this.status = status;
		this.error = error != null ? error : "Internal Server Error";
		this.message = message;
		this.path = path;
	}
}
