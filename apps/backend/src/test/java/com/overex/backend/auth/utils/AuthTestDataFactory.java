package com.overex.backend.auth.utils;

import java.util.UUID;

import com.overex.backend.auth.dto.requests.SignInAccountRequestDto;
import com.overex.backend.auth.dto.requests.SignUpAccountRequestDto;
import com.overex.backend.user.Role;
import com.overex.backend.user.User;

/**
 * Central source of reusable test fixtures.
 *
 * <p>Keeps the test classes short and consistent: every test that needs a
 * "default user" or a valid sign-up/sign-in payload gets the same well-formed
 * data from here.
 */
public final class AuthTestDataFactory {

	private AuthTestDataFactory() {
	}

	/** A fixed id so tests can assert against the {@code userId} request attribute. */
	public static final UUID DEFAULT_USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

	/**
	 * A persisted-looking user (with id and a hashed-style password) and the
	 * default {@code USER} role.
	 */
	public static User createDefaultUser() {
		return User.builder()
				.id(DEFAULT_USER_ID)
				.name("Max Mustermann")
				.email("test@mail.com")
				.phone("+43123456789")
				.password("hashed-password")
				.role(Role.USER)
				.build();
	}

	/** A valid sign-up payload matching {@link #createDefaultUser()}. */
	public static SignUpAccountRequestDto createSignUpDto() {
		return SignUpAccountRequestDto.builder()
				.name("Max Mustermann")
				.email("test@mail.com")
				.phone("+43123456789")
				.password("ClearPassword")
				.build();
	}

	/** A valid sign-in payload matching {@link #createDefaultUser()}. */
	public static SignInAccountRequestDto createSignInDto() {
		return SignInAccountRequestDto.builder()
				.email("test@mail.com")
				.password("ClearPassword")
				.build();
	}
}
