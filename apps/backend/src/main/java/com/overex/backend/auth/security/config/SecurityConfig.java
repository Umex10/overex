package com.overex.backend.auth.security.config;

import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.overex.backend.auth.dto.TkName;

/**
 * Central Spring Security configuration for the application.
 *
 * <p>
 * Wires the stateless JWT-based authentication setup: CSRF is disabled because
 * the API does not rely on session cookies, access tokens are verified by
 * Spring Security's OAuth2 resource server using only the public key, and the
 * {@code /auth/**} routes are publicly reachable so unauthenticated clients can
 * sign up, sign in and refresh tokens. CORS is opened up for the configured
 * frontend origin(s).
 *
 * @see com.overex.backend.auth.config.KeyConfig
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity // enables @PreAuthorize("hasRole('ADMIN')") and friends
public class SecurityConfig {

	/**
	 * Allowed frontend origin(s) for CORS. Comma-separated; supports wildcard
	 * patterns such as {@code https://*.vercel.app}. Defaults to the local
	 * Next.js dev server.
	 */
	@Value("${FRONTEND_URL:http://localhost:3000}")
	private String frontendUrl;

	/**
	 * Builds the decoder that verifies every incoming access token.
	 *
	 * <p>
	 * Checks the RS256 signature with the public key, the standard claims
	 * (expiry, not-before) and, additionally, that {@code type_tk} is
	 * {@code ACCESS}, so a refresh token can never be used as an access token.
	 *
	 * @return the decoder used by the resource server
	 */
	@Bean
	public JwtDecoder jwtDecoder(RSAPublicKey publicKey) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publicKey).build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefault(),
				new JwtClaimValidator<String>("type_tk", TkName.ACCESS.name()::equals)));
		return decoder;
	}

	/**
	 * Turns a verified JWT into a Spring Security {@code Authentication}.
	 *
	 * <p>
	 * Reads the authorities from the token's {@code roles} claim (e.g.
	 * {@code ["ADMIN"]}) and prefixes them with {@code ROLE_}, so that
	 * {@code @PreAuthorize("hasRole('ADMIN')")} works. Spring's default would read
	 * the {@code scope} claim instead.
	 *
	 * @return the converter used by the resource server
	 */
	@Bean
	public JwtAuthenticationConverter jwtAuthenticationConverter() {
		JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
		authorities.setAuthoritiesClaimName("roles");
		authorities.setAuthorityPrefix("ROLE_");

		JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
		converter.setJwtGrantedAuthoritiesConverter(authorities);
		return converter;
	}

	/**
	 * Assembles the application's main security filter chain.
	 *
	 * <p>
	 * Disables CSRF, opts in to CORS using the bean defined below, makes
	 * {@code /auth/**} and the Swagger UI paths public, requires authentication
	 * for every other route, declares the session policy as stateless and
	 * turns on the OAuth2 resource server for bearer JWTs.
	 *
	 * @return the built security filter chain
	 * @throws Exception when {@link HttpSecurity#build()} fails
	 */
	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http,
			JwtAuthenticationConverter jwtAuthenticationConverter) throws Exception {
		http
				.cors(Customizer.withDefaults())
				.csrf(csrf -> csrf.disable()) // safe: the API is stateless and token-based, not cookie-session-based
				.authorizeHttpRequests(auth -> auth
						.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // CORS pre-flight
						.requestMatchers("/auth/**").permitAll() // public: sign-up / sign-in / refresh
						.requestMatchers("/.well-known/jwks.json").permitAll() // public: verification key for other services
						.requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll() // public docs
						.anyRequest().authenticated()) // everything else needs a valid JWT
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.oauth2ResourceServer(oauth2 -> oauth2
						.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter)));

		return http.build();
	}

	/**
	 * Exposes a delegating password encoder so hashes carrying different
	 * algorithm prefixes (e.g. {@code {bcrypt}}) can still be matched.
	 *
	 * @return the configured {@link PasswordEncoder}
	 */
	@Bean
	public PasswordEncoder encoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	/**
	 * Exposes Spring's global {@link AuthenticationManager} so the service layer
	 * can use it for email/password sign-in.
	 *
	 * <p>
	 * Spring builds this manager itself from the single {@link UserDetailsService}
	 * bean ({@code CustomUserDetailsService}) and the {@link PasswordEncoder} bean.
	 * It is also the parent of the resource server's manager. Requires exactly one
	 * {@link UserDetailsService} in the context: with none (e.g. a test slice that
	 * does not import it) or several, Spring falls back to a proxy that delegates
	 * to this bean itself, which ends in a {@code StackOverflowError} on every
	 * rejected bearer token.
	 *
	 * @return the global authentication manager built by Spring
	 * @throws Exception when the manager cannot be built
	 */
	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	/**
	 * Configures Cross-Origin Resource Sharing (CORS) for every route.
	 *
	 * <p>
	 * Reads {@code FRONTEND_URL} (a comma-separated list of patterns) and
	 * registers it via {@code setAllowedOriginPatterns}, which is required when
	 * {@code allowCredentials} is enabled and supports wildcards such as
	 * {@code https://*.vercel.app}. The configuration permits the standard REST
	 * verbs and allows credentials so the refresh-token cookie can round-trip
	 * between the browser and the API.
	 *
	 * @return the CORS configuration source registered for {@code /**}
	 */
	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		// Split by comma to support multiple origins, trim whitespace from each entry.
		List<String> allowedOrigins = new ArrayList<>(Stream.of(frontendUrl.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toList());

		// Always permit the local development clients so tools like Swagger
		// "Try it out" work out of the box, regardless of FRONTEND_URL.
		allowedOrigins.addAll(List.of(
				"http://localhost:3000", // Next.js web (dev)
				"http://localhost:3001", // Next.js web (e2e test instance)
				"http://localhost:8082" // standalone Swagger UI container
		));

		CorsConfiguration configuration = new CorsConfiguration();
		// setAllowedOriginPatterns works with allowCredentials(true) and supports
		// wildcards.
		configuration.setAllowedOriginPatterns(allowedOrigins);
		configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
		configuration.setAllowedHeaders(List.of("*"));
		configuration.setAllowCredentials(true); // required so the browser sends/stores the refresh_tk cookie

		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}
}
