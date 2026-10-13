package com.overex.backend.auth.security;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.overex.backend.common.entities.User;

import lombok.RequiredArgsConstructor;

/**
 * Adapter that exposes a {@link User} entity to Spring Security as a
 * {@link UserDetails}.
 *
 * <p>The user's email is used as the principal's username, the encoded password
 * is forwarded unchanged and the user's role is mapped to a single
 * {@code ROLE_<NAME>} granted authority so that method-level security
 * expressions such as {@code hasRole("ADMIN")} work as expected.
 *
 * @see CustomUserDetailsService
 */
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

	private final User user;

	/**
	 * Maps the user's role to a single {@code ROLE_<NAME>} authority.
	 *
	 * @return a single-element collection containing the role authority
	 */
	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
	}

	/**
	 * Returns the encoded password as stored on the underlying user.
	 *
	 * @return the encoded password
	 */
	@Override
	public String getPassword() {
		return user.getPassword();
	}

	/**
	 * Returns the principal name used by Spring Security; this is the user's
	 * email.
	 *
	 * @return the user's email address
	 */
	@Override
	public String getUsername() {
		return user.getEmail();
	}

	/**
	 * Convenience accessor that returns the same value as {@link #getUsername()}.
	 *
	 * @return the user's email address
	 */
	public String getEmail() {
		return user.getEmail();
	}

	/**
	 * Returns the persisted identifier of the underlying user.
	 *
	 * @return the user's UUID
	 */
	public UUID getId() {
		return user.getId();
	}

}
