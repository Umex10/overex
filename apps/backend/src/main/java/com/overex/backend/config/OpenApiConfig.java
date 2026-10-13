package com.overex.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger UI configuration.
 *
 * <p>Two things happen here:
 * <ol>
 *   <li>The {@code @OpenAPIDefinition} sets the API title and a step-by-step
 *       "Getting started" description rendered at the top of the Swagger page.</li>
 *   <li>The {@code @SecurityScheme} registers a global <b>bearerAuth</b> scheme.
 *       That is what makes the green <b>Authorize 🔒</b> button appear in Swagger
 *       UI — paste an access token once and every secured request sends the
 *       {@code Authorization: Bearer ...} header automatically.</li>
 * </ol>
 *
 * <p>Endpoints opt in to the lock icon by annotating their controller/method
 * with {@code @SecurityRequirement(name = "bearerAuth")} (see
 * {@link com.overex.backend.user.MeController}). The public {@code /auth/**}
 * routes deliberately leave it off.
 */
@Configuration
@OpenAPIDefinition(
	info = @Info(
		title = "Overex API",
		version = "1.0",
		description = """
			## Getting started

			Most routes require a valid JWT access token. Follow these steps:

			**1. Create an account**
			Call `POST /auth/sign-up` with your details.
			→ If you already have an account, skip this step — sign-up returns an error when the email is already taken, which is expected.

			**2. Sign in**
			Call `POST /auth/sign-in` with your credentials.
			Copy the `accessTk` value from the response body.

			**3. Authorize**
			Click the **Authorize 🔒** button at the top of this page.
			Paste **only the token** — no `Bearer` prefix, that is added automatically.

			**4. Test routes**
			All endpoints marked with 🔒 now send the Authorization header automatically.
			Try `GET /me` to see your own account.
			"""
	)
)
@SecurityScheme(
	name = "bearerAuth",
	type = SecuritySchemeType.HTTP,
	scheme = "bearer",
	bearerFormat = "JWT",
	description = "Paste only the token — no 'Bearer ' prefix. It is added automatically by Swagger."
)
public class OpenApiConfig {
}
