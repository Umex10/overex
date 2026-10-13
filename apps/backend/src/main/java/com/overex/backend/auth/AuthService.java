package com.overex.backend.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.overex.backend.auth.dto.requests.SignUpAccountRequestDto;
import com.overex.backend.common.entities.Role;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.common.repositories.UserRepository;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Service layer responsible for sign-up and credential-authentication logic.
 *
 * <p>
 * Holds the business rules around creating a new {@link User} (unique email
 * and phone, password hashing) and delegates credential verification to the
 * Spring Security {@link AuthenticationManager}.
 *
 * @see AuthController
 * @see JwtService
 */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder encoder;
	private final AuthenticationManager authenticationManager;

	/**
	 * Registers a new user.
	 *
	 * <p>
	 * Verifies that the requested email and phone number are not already in
	 * use, hashes the plain-text password with the configured
	 * {@link PasswordEncoder} and persists the resulting {@link User}. Every new
	 * account gets the role {@link Role#USER}: nobody can register as admin.
	 *
	 * @return the persisted user entity, including the generated id
	 * @throws OwnException when the email or phone number is already taken; the
	 *                      exception carries one {@link ApiError.FieldError} per
	 *                      offending field
	 */
	public User signUp(SignUpAccountRequestDto signUpAccountRequestDto) {

		// Collect every uniqueness violation first, so the client can fix all
		// offending fields at once instead of one failed request at a time.
		List<ApiError.FieldError> fieldErrors = new ArrayList<>();
		String email = normalizeEmail(signUpAccountRequestDto.getEmail());

		if (userRepository.findByEmail(email).isPresent()) {
			fieldErrors.add(ApiError.FieldError.builder()
					.field("email")
					.message("The used email is already taken.")
					.build());
		}

		if (userRepository.findByPhone(signUpAccountRequestDto.getPhone()).isPresent()) {
			fieldErrors.add(ApiError.FieldError.builder()
					.field("phone")
					.message("The used phone is already taken.")
					.build());
		}

		if (!fieldErrors.isEmpty()) {
			throw new OwnException(fieldErrors);
		}

		String encoded = encoder.encode(signUpAccountRequestDto.getPassword());

		User newUser = User.builder()
				.name(signUpAccountRequestDto.getName())
				.email(email)
				.phone(signUpAccountRequestDto.getPhone())
				.role(Role.USER)
				.password(encoded)
				.build();

		return userRepository.save(newUser);
	}

	/**
	 * Verifies a user's credentials against the authentication manager.
	 *
	 * <p>
	 * Runs the standard Spring Security username/password authentication flow
	 * and, on success, loads the matching {@link User} so its id, email and role
	 * can be put into the issued tokens.
	 *
	 * @return the authenticated user
	 * @throws org.springframework.security.core.AuthenticationException when the
	 *                                                                   credentials
	 *                                                                   are invalid
	 */
	public User authenticate(String rawEmail, String password) {
		String email = normalizeEmail(rawEmail);

		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));

		return userRepository.findByEmail(email).orElseThrow(
				() -> new EntityNotFoundException("The user with the email: " + email + " was not found."));
	}

	/**
	 * Emails are stored and looked up trimmed and in lower case, so
	 * "Max@Mail.com " and "max@mail.com" are one account: no duplicate
	 * registrations that differ only in case, and sign-in does not fail because
	 * of a capital letter.
	 */
	private static String normalizeEmail(String email) {
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
