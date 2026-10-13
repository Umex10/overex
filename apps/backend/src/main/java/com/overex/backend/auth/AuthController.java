package com.overex.backend.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.overex.backend.auth.dto.requests.SignInAccountRequestDto;
import com.overex.backend.auth.dto.requests.SignUpAccountRequestDto;
import com.overex.backend.auth.dto.responses.AuthResponseDto;
import com.overex.backend.common.entities.User;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller responsible for handling authentication endpoints.
 *
 * <p>Exposes the public sign-up, sign-in and access-token-refresh routes. On a
 * successful sign-up or sign-in a short-lived access token is returned in the
 * response body while the long-lived refresh token is written into an HTTP-only
 * cookie. The refresh endpoint reads that cookie to issue a fresh access token
 * without re-prompting the user for credentials.
 *
 * @see AuthService
 * @see JwtService
 */
@Tag(name = "Auth", description = "Sign-up, sign-in and token refresh — no authentication required")
@RestController
@RequestMapping(path = "/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;
	private final JwtService jwtService;

	/**
	 * Handles user registration requests.
	 *
	 * <p>Persists a new user from the supplied payload, then issues an access
	 * token and attaches a refresh-token cookie to the response so the client is
	 * authenticated immediately after sign-up.
	 *
	 * @return the authentication response containing the access token and its
	 *         lifetime, with HTTP status 201 (Created)
	 */
	@Operation(
		summary = "Create a new account",
		description = "Registers a new user and returns an access token. "
			+ "Returns a 400 with field errors if the email or phone is already taken — in that case use sign-in instead."
	)
	@PostMapping(path = "/sign-up")
	public ResponseEntity<AuthResponseDto> signUp(
			@Valid @RequestBody SignUpAccountRequestDto signUpAccountRequestDto,
			HttpServletRequest request,
			HttpServletResponse response) {

		User user = authService.signUp(signUpAccountRequestDto);

		AuthResponseDto authResponseDto = createAuthResponseDto(user, response);

		return new ResponseEntity<>(authResponseDto, HttpStatus.CREATED);
	}

	/**
	 * Handles user sign-in requests.
	 *
	 * <p>Authenticates the supplied credentials against the configured
	 * {@code AuthenticationManager}; on success a new access token is issued and
	 * the refresh-token cookie is attached to the response.
	 *
	 * @return the authentication response containing the access token and its
	 *         lifetime
	 */
	@Operation(
		summary = "Sign in to an existing account",
		description = "Verifies the credentials and returns an access token. Returns 401 when the credentials are wrong."
	)
	@PostMapping(path = "/sign-in")
	public ResponseEntity<AuthResponseDto> signIn(
			@Valid @RequestBody SignInAccountRequestDto signInAccountRequestDto,
			HttpServletRequest request,
			HttpServletResponse response) {

		User user = authService.authenticate(
				signInAccountRequestDto.getEmail(),
				signInAccountRequestDto.getPassword());

		AuthResponseDto authResponseDto = createAuthResponseDto(user, response);

		return ResponseEntity.ok(authResponseDto);
	} 

	/**
	 * Issues a fresh access token from a valid refresh-token cookie.
	 *
	 * <p>Reads the {@code refresh_tk} cookie from the request, validates it via
	 * {@link JwtService#validateRefreshTk(String)} (which also loads the user) and
	 * returns a new access token for that user. The refresh cookie itself is not
	 * rotated.
	 *
	 * @return the authentication response containing the new access token, or
	 *         HTTP 401 (Unauthorized) when the refresh cookie is missing
	 */
	@Operation(
		summary = "Refresh the access token",
		description = "Exchanges the refresh_tk cookie for a new short-lived access token. Returns 401 when the cookie is missing."
	)
	@GetMapping(path = "/access-tk")
	public ResponseEntity<?> createAccessTk(HttpServletRequest request, HttpServletResponse response) {

		String refreshTk = jwtService.extractRefreshTk(request);

		if (refreshTk == null || refreshTk.isEmpty()) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body("The refresh token is missing");
		}

		User user = jwtService.validateRefreshTk(refreshTk);

		AuthResponseDto authResponseDto = createAuthResponseDto(user, null);

		return ResponseEntity.ok(authResponseDto);
	}

	/**
	 * Builds the standard authentication response and, when a response is
	 * supplied, attaches the refresh-token cookie.
	 *
	 * <p>Pass {@code null} for the response argument to skip cookie creation,
	 * which is what the refresh endpoint does because the client already holds a
	 * valid cookie at that point.
	 *
	 * @return the populated {@link AuthResponseDto}
	 */
	private AuthResponseDto createAuthResponseDto(User user, HttpServletResponse response) {

		String accessTk = jwtService.createAccessTk(user);

		if (response != null) {
			String refreshTk = jwtService.createRefreshTk(user);

			Cookie refreshCookie = new Cookie("refresh_tk", refreshTk);
			refreshCookie.setHttpOnly(true);   // not readable from JavaScript -> mitigates XSS token theft
			refreshCookie.setSecure(false);    // set to true behind HTTPS in production
			refreshCookie.setPath("/");
			refreshCookie.setMaxAge(30 * 24 * 60 * 60); // 30 days, matches the refresh-token lifetime

			response.addCookie(refreshCookie);
		}

		return AuthResponseDto.builder()
				.accessTk(accessTk)
				.expiresIn(15 * 60L) // 900 seconds, matches the access-token lifetime
				.build();
	}

}
