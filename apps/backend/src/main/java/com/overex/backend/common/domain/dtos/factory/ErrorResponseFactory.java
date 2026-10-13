package com.overex.backend.common.domain.dtos.factory;

import com.overex.backend.common.domain.dtos.ErrorResponse;

/**
 * Static factory helpers for building the most common {@link ErrorResponse}
 * variants.
 *
 * <p>Keeps the {@link com.overex.backend.common.exception.GlobalExceptionHandler}
 * concise by hiding the repeated boilerplate of choosing the right status,
 * reason phrase and request path.
 */
public final class ErrorResponseFactory {

	private ErrorResponseFactory() {
		// utility class — not instantiable
	}

	/**
	 * Builds a 500 response with an explicit message and request path.
	 *
	 * @return the assembled error response
	 */
	public static ErrorResponse internalServerError(String message, String path) {
		return new ErrorResponse(500, "Internal Server Error", message, path);
	}

	/**
	 * Builds a 404 response with an explicit message and request path.
	 *
	 * @return the assembled error response
	 */
	public static ErrorResponse notFound(String message, String path) {
		return new ErrorResponse(404, "Not Found", message, path);
	}
}
