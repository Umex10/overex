package com.overex.backend.auth.security;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.overex.backend.common.entities.User;
import com.overex.backend.common.repositories.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service that resolves Spring Security principals from the application's user
 * store.
 *
 * <p>Plugs the {@link UserRepository} into Spring Security's standard user
 * lookup contract by wrapping the loaded {@link User} entity in a
 * {@link CustomUserDetails} adapter.
 *
 * @see CustomUserDetails
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final UserRepository userRepository;

	/**
	 * Loads a user by email (the value Spring Security treats as the username).
	 *
	 * @return a {@link CustomUserDetails} that adapts the persisted user entity
	 * @throws UsernameNotFoundException when no user with that email exists
	 */
	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException(
						"User with the email: " + email + " doesn't exist."));
		return new CustomUserDetails(user);
	}

}
