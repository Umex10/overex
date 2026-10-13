package com.overex.backend.user;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.overex.backend.common.entities.User;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.user.dto.UserResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Example controller for the <b>protected</b> side of the API.
 *
 * <p>Every route here requires a valid access token. It exists to demonstrate
 * the two building blocks Overex gives you for your own feature controllers:
 *
 * <ul>
 *   <li><b>{@code @AuthenticationPrincipal Jwt jwt}</b> — the already verified
 *       access token. {@code jwt.getSubject()} is the user id; other claims
 *       ({@code email}, {@code roles}) are available without a database lookup.</li>
 *   <li><b>{@code @PreAuthorize("hasRole('ADMIN')")}</b> — method-level role
 *       checks, powered by the {@code ROLE_<NAME>} authorities that
 *       {@link com.overex.backend.auth.security.config.SecurityConfig} builds
 *       from the token's {@code roles} claim.</li>
 * </ul>
 *
 * <p>The {@code @SecurityRequirement} annotation is what makes these endpoints
 * show the 🔒 icon in Swagger UI.
 */
@Tag(name = "Me", description = "Protected example routes — require a valid access token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class MeController {

	private final UserRepository userRepository;

	/**
	 * Returns the currently authenticated user.
	 *
	 * @param jwt the verified access token, injected by Spring Security
	 * @return the password-free projection of the signed-in user
	 */
	@Operation(summary = "Get the current user", description = "Returns the account behind the supplied access token.")
	@GetMapping("/me")
	public ResponseEntity<UserResponseDto> me(@AuthenticationPrincipal Jwt jwt) {
		UUID userId = UUID.fromString(jwt.getSubject());
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User " + userId + " no longer exists."));
		return ResponseEntity.ok(UserResponseDto.from(user));
	}

	/**
	 * Admin-only smoke-test route.
	 *
	 * <p>Returns 200 for users whose role is {@code ADMIN} and 403 for everyone
	 * else — a one-line demonstration of role-based authorization.
	 *
	 * @return a short confirmation string
	 */
	@Operation(summary = "Admin-only ping", description = "Succeeds only for ADMIN users; returns 403 otherwise.")
	@PreAuthorize("hasRole('ADMIN')")
	@GetMapping("/admin/ping")
	public ResponseEntity<String> adminPing() {
		return ResponseEntity.ok("pong — you are an ADMIN 👑");
	}
}
