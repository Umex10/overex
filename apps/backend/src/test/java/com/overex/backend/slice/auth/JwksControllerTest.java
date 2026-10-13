package com.overex.backend.slice.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.overex.backend.auth.JwksController;
import com.overex.backend.auth.security.CustomUserDetailsService;
import com.overex.backend.auth.security.config.SecurityConfig;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.utils.TestKeys;

/**
 * Web-layer test for {@link JwksController}, through the real
 * {@link SecurityConfig} filter chain.
 *
 * <p>
 * Checks that the route is reachable without a token ({@code permitAll}), that
 * the public key is published as a JWK set with the expected fields and that no
 * private key material ever leaves the service.
 */
@WebMvcTest(JwksController.class)
@Import({ SecurityConfig.class, CustomUserDetailsService.class, TestKeys.PublicKeyConfig.class })
public class JwksControllerTest {

	@Autowired
	private MockMvc mockMvc;

	/** Needed by {@link CustomUserDetailsService}, which SecurityConfig's AuthenticationManager requires. */
	@MockitoBean
	private UserRepository userRepository;

	/** Without a token, the JWKS contains exactly one RSA signing key (n, e) and nothing private. */
	@Test
	void shouldCreateJwksJson() throws Exception {
		mockMvc.perform(get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.keys").isArray())
				.andExpect(jsonPath("$.keys.length()").value(1))
				.andExpect(jsonPath("$.keys[0].kty").value("RSA"))
				.andExpect(jsonPath("$.keys[0].use").value("sig"))
				.andExpect(jsonPath("$.keys[0].alg").value("RS256"))
				.andExpect(jsonPath("$.keys[0].n").isNotEmpty())
				.andExpect(jsonPath("$.keys[0].e").value("AQAB"))
				// "d" is the private exponent: it must never be published
				.andExpect(jsonPath("$.keys[0].d").doesNotExist());
	}
}
