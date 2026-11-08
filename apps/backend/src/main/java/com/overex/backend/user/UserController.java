package com.overex.backend.user;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.overex.backend.event.Event;
import com.overex.backend.user.dto.UserResponseDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Routes around the signed-in user. Every route here requires a valid access
 * token; {@code @AuthenticationPrincipal Jwt jwt} is the already verified
 * token, and {@code jwt.getSubject()} is the user id.
 *
 * <p>The {@code @SecurityRequirement} annotation is what makes these endpoints
 * show the 🔒 icon in Swagger UI.
 */
@Tag(name = "User", description = "Routes of the signed-in user, require a valid access token")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

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
		User user = userService.getCurrentUser(userId);
		return ResponseEntity.ok(UserResponseDto.from(user));
	}

}
