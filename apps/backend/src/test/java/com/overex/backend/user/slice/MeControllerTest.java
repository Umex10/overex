package com.overex.backend.user.slice;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.overex.backend.auth.JwtService;
import com.overex.backend.auth.security.CustomUserDetailsService;
import com.overex.backend.auth.security.config.SecurityConfig;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.user.MeController;
import com.overex.backend.auth.utils.AuthTestDataFactory;
import com.overex.backend.auth.utils.TestKeys;
import com.overex.backend.auth.utils.TkFactory;

/**
 * Web-layer test for the protected {@link MeController} routes, exercised
 * through the real {@link SecurityConfig} filter chain (incl. method security).
 *
 * <p>
 * Two kinds of requests are used:
 * <ul>
 * <li>{@code authenticatedAs(...)} puts an already verified JWT into the
 * security context ({@code jwt()} from spring-security-test). This checks the
 * controller and the {@code @PreAuthorize} rules without token
 * verification.</li>
 * <li>Real tokens from {@link JwtService}, signed with {@link TestKeys}, sent
 * in the {@code Authorization} header. They pass through the whole chain
 * (filter, JwtDecoder, converter), so token verification is covered too.</li>
 * </ul>
 */
@WebMvcTest(MeController.class)
@Import({ SecurityConfig.class, CustomUserDetailsService.class, TestKeys.PublicKeyConfig.class })
public class MeControllerTest {

	@Autowired
	private MockMvc mockMvc;

	/** Needed by {@link CustomUserDetailsService}, and stubbed for /me lookups. */
	@MockitoBean
	private UserRepository userRepository;

	/** Only a tool to create real tokens; built by hand, not a Spring bean. */
	private JwtService jwtService;

	private final User user = AuthTestDataFactory.createDefaultUser();

	private final User admin = AuthTestDataFactory.createAdminUser();

	/**
	 * Builds the token tool with the same key pair whose public key the
	 * JwtDecoder uses ({@link TestKeys.PublicKeyConfig}).
	 */
	@BeforeEach
	void setUp() {
		jwtService = new JwtService(TestKeys.privateKey(), TestKeys.publicKey(), userRepository);
	}

	/**
	 * The controller reads the user id from the token subject and returns the user.
	 */
	@Test
	void shouldReturnCurrentUser() throws Exception {
		when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

		mockMvc.perform(get("/me").with(TkFactory.authenticatedAs(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(user.getEmail()))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	/**
	 * A valid token whose user was deleted meanwhile: the controller throws
	 * {@code EntityNotFoundException}, the global handler answers 404 in the common
	 * error envelope.
	 */
	@Test
	void shouldReturn404WhenUserNoLongerExists() throws Exception {
		when(userRepository.findById(user.getId())).thenReturn(Optional.empty());

		mockMvc.perform(get("/me").with(TkFactory.authenticatedAs(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.error").value("Not Found"))
				.andExpect(jsonPath("$.message").value("User " + user.getId() + " no longer exists."))
				.andExpect(jsonPath("$.path").value("/me"));
	}

	/** Without a token the request is rejected. */
	@Test
	void shouldRejectAnonymousAccessToMe() throws Exception {
		mockMvc.perform(get("/me"))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * A correctly signed refresh token in the {@code Authorization} header fails
	 * the {@code type_tk == ACCESS} check in the JwtDecoder; the controller is
	 * never reached.
	 */
	@Test
	void shouldRejectRefreshTkUsedAsAccessTk() throws Exception {
		String refreshTk = jwtService.createRefreshTk(user);

		mockMvc.perform(get("/me")
				.header("Authorization", "Bearer " + refreshTk))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string("WWW-Authenticate", containsString("invalid_token")));

		// proves the request was stopped before the controller looked up the user
		verifyNoInteractions(userRepository);
	}

	/** A regular user gets 403 on the admin route. */
	@Test
	void shouldForbidAdminRouteForRegularUser() throws Exception {
		mockMvc.perform(get("/admin/ping").with(TkFactory.authenticatedAs(user)))
				.andExpect(status().isForbidden());
	}

	/** An admin may use the admin route. */
	@Test
	void shouldAllowAdminRouteForAdmin() throws Exception {
		mockMvc.perform(get("/admin/ping").with(TkFactory.authenticatedAs(admin)))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("pong")));
	}

	/**
	 * A real admin access token passes the JwtDecoder; the converter turns the
	 * {@code roles: ["ADMIN"]} claim into {@code ROLE_ADMIN}, so
	 * {@code @PreAuthorize("hasRole('ADMIN')")} lets it through.
	 */
	@Test
	void shouldMapRolesClaimToAdminAuthority() throws Exception {
		String accessTk = jwtService.createAccessTk(admin);

		mockMvc.perform(get("/admin/ping")
				.header("Authorization", "Bearer " + accessTk))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("pong")));
	}

	/**
	 * A real user access token carries {@code roles: ["USER"]}; the converter
	 * maps it to {@code ROLE_USER} only, so the admin route answers 403.
	 */
	@Test
	void shouldNotGrantAdminToUserToken() throws Exception {
		String accessTk = jwtService.createAccessTk(user);

		mockMvc.perform(get("/admin/ping")
				.header("Authorization", "Bearer " + accessTk))
				.andExpect(status().isForbidden());
	}
}
