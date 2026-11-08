package com.overex.backend.auth.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

/**
 * Request payload for the {@code POST /auth/sign-in} endpoint.
 *
 * <p>Carries the credentials a returning user submits to obtain a fresh
 * access/refresh-token pair. Both fields are validated server-side via Jakarta
 * Bean Validation; failed validation is surfaced as HTTP 400 with field-level
 * error messages.
 *
 * <p>The password has no length rule: sign-in only has to tell right from
 * wrong, and a length hint would reveal more than the generic "credentials are
 * incorrect".
 */
@Builder
public record SignInAccountRequestDto(

		@NotBlank(message = "Email is required")
		@Email(message = "Email must be valid")
		String email,

		@NotBlank(message = "Password is required")
		String password) {
}
