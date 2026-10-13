package com.overex.backend.unit.auth.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.overex.backend.auth.security.CustomUserDetails;
import com.overex.backend.auth.security.CustomUserDetailsService;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.utils.TestDataFactory;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 *
 * <p>
 * Pure Mockito tests: no Spring context. This is the lookup the
 * {@code DaoAuthenticationProvider} uses on email/password sign-in, so the
 * tests check that a known email becomes a {@link CustomUserDetails} (email as
 * username, password hash, {@code ROLE_} authority) and that an unknown email
 * throws {@link UsernameNotFoundException}.
 */
@ExtendWith(MockitoExtension.class)
public class CustomUserDetailsServiceTest {

	@Mock
	private UserRepository userRepository;

	@InjectMocks
	private CustomUserDetailsService customUserDetailsService;

	private final User user = TestDataFactory.createDefaultUser();

	/**
	 * A known email is turned into a {@link CustomUserDetails} that carries the
	 * email as username, the stored password hash, the id and the user's role as
	 * {@code ROLE_} authority.
	 */
	@Test
	void shouldLoadUserByUsername() {
		when(userRepository.findByEmail(any())).thenReturn(Optional.of(user));

		UserDetails found = customUserDetailsService.loadUserByUsername(user.getEmail());

		CustomUserDetails details = assertInstanceOf(CustomUserDetails.class, found);
		assertEquals(user.getEmail(), details.getUsername());
		assertEquals(user.getEmail(), details.getEmail());
		assertEquals(user.getPassword(), details.getPassword());
		assertEquals(user.getId(), details.getId());
		assertEquals(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())),
				details.getAuthorities());
	}

	/** An unknown email throws {@link UsernameNotFoundException}, naming the email. */
	@Test
	void shouldNotLoadUserByUsername() {
		when(userRepository.findByEmail(any())).thenReturn(Optional.empty());

		UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
				() -> customUserDetailsService.loadUserByUsername(user.getEmail()));

		assertEquals("User with the email: " + user.getEmail() + " doesn't exist.", ex.getMessage());
	}

}
