package com.overex.backend.auth.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.overex.backend.auth.AuthService;
import com.overex.backend.auth.dto.requests.SignUpAccountRequestDto;
import com.overex.backend.common.entities.Role;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.auth.utils.AuthTestDataFactory;

import jakarta.persistence.EntityNotFoundException;

/**
 * Unit tests for {@link AuthService}.
 *
 * <p>
 * Pure Mockito tests: no Spring context. Covers sign-up (unique email and phone
 * lead to a persisted, hashed user; taken fields lead to an
 * {@link OwnException} with one field error each) and sign-in (valid
 * credentials return the user, invalid ones throw, and a user that vanishes
 * after the credential check leads to an {@link EntityNotFoundException}).
 */
@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

	@Mock
	UserRepository userRepository;

	@Mock
	PasswordEncoder encoder;

	@Mock
	AuthenticationManager authenticationManager;

	@InjectMocks
	AuthService authService;

	private final User user = AuthTestDataFactory.createDefaultUser();

	/** A new user with a free email and phone is saved with the hashed password. */
	@Test
	void shouldSignUpAccount() {
		SignUpAccountRequestDto dto = AuthTestDataFactory.createSignUpDto();

		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());
		when(userRepository.findByPhone(user.getPhone())).thenReturn(Optional.empty());
		// save() returns the entity it was given, like the real repository does
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
		// The encoder turns the clear password into "hashed-password", so the last
		// assert proves the hash is stored, not the clear text.
		when(encoder.encode(dto.getPassword())).thenReturn(user.getPassword());

		User savedUser = authService.signUp(dto);

		assertNotNull(savedUser);
		assertEquals(user.getEmail(), savedUser.getEmail());
		assertEquals(user.getPassword(), savedUser.getPassword());
		assertEquals(Role.USER, savedUser.getRole()); // sign-up never creates an admin
	}

	/** The email is stored trimmed and in lower case, and checked for duplicates that way. */
	@Test
	void shouldNormalizeEmailOnSignUp() {
		SignUpAccountRequestDto dto = AuthTestDataFactory.createSignUpDto();
		dto.setEmail("  Test@MAIL.com ");

		when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.empty());
		when(userRepository.findByPhone(user.getPhone())).thenReturn(Optional.empty());
		when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

		assertEquals("test@mail.com", authService.signUp(dto).getEmail());
	}

	/** Sign-in normalizes the email the same way before checking the credentials. */
	@Test
	void shouldNormalizeEmailOnSignIn() {
		when(userRepository.findByEmail("test@mail.com")).thenReturn(Optional.of(user));

		authService.authenticate(" TEST@mail.com", user.getPassword());

		verify(authenticationManager)
				.authenticate(new UsernamePasswordAuthenticationToken("test@mail.com", user.getPassword()));
	}

	/**
	 * A missing email stays null instead of failing in normalizeEmail; rejecting
	 * it is the job of bean validation on the request DTO.
	 */
	@Test
	void shouldPassNullEmailThroughNormalization() {
		when(userRepository.findByEmail(null)).thenReturn(Optional.of(user));

		authService.authenticate(null, user.getPassword());

		verify(authenticationManager)
				.authenticate(new UsernamePasswordAuthenticationToken(null, user.getPassword()));
	}

	/** Taken email and phone are both reported at once. */
	@Test
	void shouldNotSignUpAccount() {
		SignUpAccountRequestDto dto = AuthTestDataFactory.createSignUpDto();

		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
		when(userRepository.findByPhone(user.getPhone())).thenReturn(Optional.of(user));

		OwnException exception = assertThrows(OwnException.class, () -> authService.signUp(dto));

		List<ApiError.FieldError> errors = exception.getErrors();
		assertEquals(2, errors.size());
		assertTrue(errors.stream().anyMatch(e -> e.getField().equals("email")));
		assertTrue(errors.stream().anyMatch(e -> e.getField().equals("phone")));
	}

	/**
	 * Valid credentials return the user loaded by email. The AuthenticationManager
	 * mock needs no stub: returning normally (null) is the success case.
	 */
	@Test
	void shouldAuthenticate() {
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));

		User returnedUser = authService.authenticate(user.getEmail(), user.getPassword());

		assertEquals(user.getId(), returnedUser.getId());
		assertEquals(user.getEmail(), returnedUser.getEmail());
	}

	/**
	 * Wrong credentials make the AuthenticationManager throw, and the error is
	 * passed on.
	 */
	@Test
	void shouldNotAuthenticate() {
		when(authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(user.getEmail(), user.getPassword())))
				.thenThrow(new BadCredentialsException("Bad credentials"));

		assertThrows(org.springframework.security.core.AuthenticationException.class,
				() -> authService.authenticate(user.getEmail(), user.getPassword()));
	}

	/**
	 * Edge case: the credentials are accepted, but the user is gone by the time
	 * it is loaded again (e.g. deleted in between) &rarr;
	 * {@link EntityNotFoundException}.
	 */
	@Test
	void shouldNotFindUserByEmailAfterSuccessfullAuthentication() {
		when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.empty());

		assertThrows(EntityNotFoundException.class,
				() -> authService.authenticate(user.getEmail(), user.getPassword()));
	}
}
