package com.overex.backend.auth.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;

import com.overex.backend.auth.JwtService;
import com.overex.backend.auth.config.RsaKeyFiles;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.auth.utils.AuthTestDataFactory;
import com.overex.backend.auth.utils.TestRsaKeys;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.SignatureException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.Cookie;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>
 * Pure Mockito tests: no Spring context, so the service is built by hand with
 * {@code new}. Keys come from {@link TestRsaKeys}, so no key file is needed. Token
 * contents are read with nimbus-jose-jwt ({@link SignedJWT#parse(String)}),
 * which only decodes and does not verify.
 */
@ExtendWith(MockitoExtension.class)
public class JwtServiceTest {

	/** Recreated by Mockito before every test, so stubs never leak between tests. */
	@Mock
	private UserRepository userRepository;

	private JwtService jwtService;

	private final User user = AuthTestDataFactory.createDefaultUser();

	/**
	 * Builds a fresh service per test so it always uses the current test's
	 * {@code userRepository} mock.
	 */
	@BeforeEach
	void setUp() {
		jwtService = new JwtService(TestRsaKeys.privateKey(), TestRsaKeys.publicKey(), userRepository);
	}

	/** An access token carries the user id as {@code sub} plus the {@code email} and {@code roles} claims. */
	@Test
	void shouldCreateAccessTk() throws ParseException {
		String tk = jwtService.createAccessTk(user);
		assertNotNull(tk);

		JWTClaimsSet claims = SignedJWT.parse(tk).getJWTClaimsSet();

		assertEquals(user.getId().toString(), claims.getSubject());
		assertEquals(user.getEmail(), claims.getStringClaim("email"));
		assertTrue(claims.getStringListClaim("roles").contains(user.getRole().name()));
	}

	/** A refresh token carries only the user id; email and roles are re-read from the database on refresh. */
	@Test
	void shouldCreateRefreshTk() throws ParseException {
		String tk = jwtService.createRefreshTk(user);
		assertNotNull(tk);

		JWTClaimsSet claims = SignedJWT.parse(tk).getJWTClaimsSet();

		assertEquals(user.getId().toString(), claims.getSubject());
		assertNull(claims.getStringClaim("email"));
		assertNull(claims.getStringListClaim("roles"));
	}

	/** A valid refresh token resolves to the user behind its subject, loaded by id. */
	@Test
	void shouldValidateRefreshTk() {
		String tk = jwtService.createRefreshTk(user);
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		User result = jwtService.validateRefreshTk(tk);

		assertEquals(user, result);
		verify(userRepository).findById(user.getId());
	}

	/** A refresh token signed with an unrelated private key fails the signature check. */
	@Test
	void shouldRejectRefreshTkSignedWithAnotherKey() {
		KeyPair otherPair = RsaKeyFiles.generateKeyPair();
		JwtService otherService = new JwtService(
				(RSAPrivateKey) otherPair.getPrivate(),
				(RSAPublicKey) otherPair.getPublic(),
				userRepository);

		String forgedTk = otherService.createRefreshTk(user);

		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
				() -> jwtService.validateRefreshTk(forgedTk));
		// translated into a Spring AuthenticationException (-> 401); the JJWT cause stays visible
		assertInstanceOf(SignatureException.class, ex.getCause());
	}

	/** A correctly signed refresh token whose {@code exp} lies in the past is rejected. */
	@Test
	void shouldRejectExpiredRefreshTk() {
		// JwtService always sets an expiry in the future, so the expired token is
		// built directly with jjwt.
		String expiredTk = Jwts.builder()
				.setSubject(user.getId().toString())
				.claim("type_tk", "REFRESH")
				.setExpiration(new Date(System.currentTimeMillis() - 1000))
				.signWith(TestRsaKeys.privateKey(), SignatureAlgorithm.RS256) // right key: only the expiry is wrong
				.compact();

		BadCredentialsException ex = assertThrows(BadCredentialsException.class,
				() -> jwtService.validateRefreshTk(expiredTk));
		assertInstanceOf(ExpiredJwtException.class, ex.getCause());
	}

	/** Something that is no JWT at all (e.g. a tampered cookie) is rejected the same way. */
	@Test
	void shouldRejectMalformedRefreshTk() {
		assertThrows(BadCredentialsException.class, () -> jwtService.validateRefreshTk("not-a-jwt"));
	}

	/** An access token sent as refresh token fails the {@code type_tk == REFRESH} check. */
	@Test
	void shouldRejectAccessTkUsedAsRefreshTk() {
		String accessTk = jwtService.createAccessTk(user);

		assertThrows(BadCredentialsException.class, () -> jwtService.validateRefreshTk(accessTk));
	}

	/** A refresh token of a user that no longer exists is rejected. */
	@Test
	void shouldThrowExceptionWhenUserNotFound() {
		String tk = jwtService.createRefreshTk(user);
		when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class, () -> jwtService.validateRefreshTk(tk));
	}

	/** The refresh token is read from the {@code refresh_tk} cookie. */
	@Test
	void shouldExtractRefreshTk() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setCookies(new Cookie("refresh_tk", "New-refresh-tk"));

		assertEquals("New-refresh-tk", jwtService.extractRefreshTk(request));
	}

	/** Without the cookie there is no refresh token. */
	@Test
	void shouldNotExtractRefreshTk() {
		MockHttpServletRequest request = new MockHttpServletRequest();

		assertNull(jwtService.extractRefreshTk(request));
	}
}
