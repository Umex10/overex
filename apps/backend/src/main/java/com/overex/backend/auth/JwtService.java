package com.overex.backend.auth;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;

import com.overex.backend.auth.dto.TkName;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.repositories.UserRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

/**
 * Service layer responsible for JWT issuance and refresh-token validation.
 *
 * <p>
 * Tokens are signed with RS256: this service signs with the private key and
 * verifies refresh tokens with the public key (both provided by
 * {@link com.overex.backend.auth.config.KeyConfig}). Access tokens are
 * verified on every request by Spring Security's OAuth2 resource server (see
 * {@link com.overex.backend.auth.security.config.SecurityConfig}), not here.
 *
 * <p>
 * Access tokens carry {@code sub} (user id), {@code email} and {@code roles};
 * refresh tokens only carry {@code sub}. Refresh tokens are read from the
 * {@code refresh_tk} cookie.
 *
 * @see AuthController
 */
@Service
@RequiredArgsConstructor
public class JwtService {

	private final RSAPrivateKey privateKey;
	private final RSAPublicKey publicKey;

	/** Refresh-token lifetime: 30 days (in milliseconds). */
	private final long REFRESH_TK = 1000L * 60 * 60 * 24 * 30;

	/** Access-token lifetime: 15 minutes (in milliseconds). */
	private final long ACCESS_TK = 1000L * 60 * 15;

	private final UserRepository userRepository;

	/**
	 * Validates a refresh token (signature, expiry and token type) and loads
	 * the user it belongs to from the database.
	 *
	 * @return the user whose id is the token's subject
	 * @throws io.jsonwebtoken.JwtException when the token is malformed, expired
	 *                                      or signed with a different key
	 */
	public User validateRefreshTk(String tk) {
		UUID userId = UUID.fromString(extractUserId(tk));
		return userRepository.findById(userId).orElseThrow(
				() -> new EntityNotFoundException("The user with the id: " + userId + " was not found."));
	}

	/**
	 * Creates a short-lived access token carrying the user's id, email and roles.
	 *
	 * @return the signed access JWT
	 */
	public String createAccessTk(User user) {
		return createTk(user, TkName.ACCESS);
	}

	/**
	 * Creates a long-lived refresh token carrying only the user's id.
	 *
	 * @return the signed refresh JWT
	 */
	public String createRefreshTk(User user) {
		return createTk(user, TkName.REFRESH);
	}

	/**
	 * Extracts the refresh token from the {@code refresh_tk} cookie.
	 *
	 * @return the cookie value, or {@code null} when the request carries no
	 *         cookies or no cookie with that name
	 */
	public String extractRefreshTk(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}

		return Arrays.stream(cookies)
				.filter(cookie -> "refresh_tk".equals(cookie.getName()))
				.map(Cookie::getValue)
				.findFirst()
				.orElse(null);
	}

	/**
	 * Builds and signs a JWT for the supplied user with the lifetime
	 * matching the given token type.
	 *
	 * <p>
	 * The token carries the subject (the user's id), an issued-at
	 * timestamp, the expiration timestamp and a {@code type_tk} claim that
	 * distinguishes access tokens from refresh tokens. Access tokens
	 * additionally carry the {@code email} and {@code roles} claims.
	 *
	 * @return the compact, signed JWT string
	 */
	private String createTk(User user, TkName tkType) {

		long expiryMs = (tkType == TkName.ACCESS) ? ACCESS_TK : REFRESH_TK;

		Map<String, Object> claims = new HashMap<>();
		if (tkType == TkName.ACCESS) {
			claims.put("email", user.getEmail());
			claims.put("roles", List.of(user.getRole().name()));
		}
		return Jwts.builder()
				.setClaims(claims)
				.setSubject(user.getId().toString())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.claim("type_tk", tkType) // lets the receiver tell access from refresh tokens apart
				.setExpiration(new Date(System.currentTimeMillis() + expiryMs))
				.signWith(privateKey, SignatureAlgorithm.RS256) // our tamper-proof checksum
				.compact(); // produces the xxxxx.yyyyy.zzzzz string
	}

	/**
	 * Parses a refresh token with the public key and returns its subject claim
	 * (the user's id).
	 *
	 * @return the user id encoded as the token's subject
	 * @throws io.jsonwebtoken.JwtException when the token cannot be parsed or
	 *                                      the signature is invalid
	 */
	private String extractUserId(String refreshTk) {
		Claims claims;
		try {
			claims = Jwts.parserBuilder()
					.setSigningKey(publicKey)
					.build()
					.parseClaimsJws(refreshTk)
					.getBody();
		} catch (JwtException | IllegalArgumentException ex) {
			// Wrong signature, expired, malformed: a client error, not a server error.
			// As an AuthenticationException it becomes a 401 instead of the catch-all 500.
			throw new BadCredentialsException("Invalid refresh token", ex);
		}

		if (!TkName.REFRESH.name().equals(claims.get("type_tk", String.class))) {
			throw new BadCredentialsException("Not a refresh token");
		}

		return claims.getSubject();
	}
}
