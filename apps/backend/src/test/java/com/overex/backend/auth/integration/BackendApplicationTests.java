package com.overex.backend.auth.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.overex.backend.auth.dto.TkName;
import com.overex.backend.auth.dto.requests.SignInAccountRequestDto;
import com.overex.backend.auth.dto.requests.SignUpAccountRequestDto;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.auth.utils.AuthTestDataFactory;
import com.overex.backend.auth.utils.TestKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.jwk.JWKSet;

import jakarta.servlet.http.Cookie;

/**
 * Integration tests: the whole application runs (security filter chain,
 * controllers, services, JPA) against the in-memory H2 database of the "test"
 * profile. Nothing is mocked.
 *
 * <p>
 * Each test plays a flow the way a client would: sign up, sign in, use the
 * issued tokens. That checks what slice tests cannot see, e.g. that a password
 * stored by sign-up is recognised by sign-in, and that a token issued by the
 * real {@code JwtService} is accepted by the real resource server.
 *
 * <p>
 * The real {@code KeyConfig} is loaded here, so it reads the JWT keys from the
 * files named by {@code jwt.private-key-location} and
 * {@code jwt.public-key-location}. The test writes the key pair of
 * {@link TestKeys} into a temporary folder and points both properties at it,
 * so no local {@code keys/} folder is needed (e.g. on the CI runner).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class BackendApplicationTests {

	/**
	 * Temporary folder for the generated key files. JUnit creates it before the
	 * Spring context starts and deletes it after all tests of this class. It is
	 * static because {@link #keyLocations} below is static and runs before any
	 * test instance exists.
	 */
	@TempDir
	static Path keyDir;

	/**
	 * Runs while Spring builds the context, before the beans are created: writes
	 * {@code private.pem} and {@code public.pem} into {@link #keyDir} and sets the
	 * two key-location properties to these files. Values registered here win over
	 * {@code application.properties} and {@code application-test.properties}.
	 */
	@DynamicPropertySource
	static void keyLocations(DynamicPropertyRegistry registry) {
		TestKeys.writePemFiles(keyDir);
		registry.add("jwt.private-key-location", () -> keyDir.resolve("private.pem").toUri().toString());
		registry.add("jwt.public-key-location", () -> keyDir.resolve("public.pem").toUri().toString());
	}

	/** Sends requests through the whole application, like a client would. */
	@Autowired
	private MockMvc mockMvc;

	/** Turns the request DTOs into JSON. */
	@Autowired
	private ObjectMapper objectMapper;

	/** The real repository (no mock), used to reset the database and to create an admin. */
	@Autowired
	private UserRepository userRepository;

	/** Hashes the admin's password the same way sign-up would. */
	@Autowired
	private PasswordEncoder passwordEncoder;

	/**
	 * The H2 database lives as long as the Spring context, i.e. across tests.
	 * Emptying it before every test keeps the tests independent of each other.
	 */
	@BeforeEach
	void emptyDatabase() {
		userRepository.deleteAll();
	}

	/**
	 * Sign-up stores the user, sign-in recognises the hashed password and issues
	 * an access token, and /me accepts that token and returns the stored account.
	 */
	@Test
	void shouldReturnOwnAccountWithTokenFromSignIn() throws Exception {
		SignUpAccountRequestDto signUp = AuthTestDataFactory.createSignUpDto();
		signUp(signUp);

		String accessTk = accessTkOf(signIn(AuthTestDataFactory.createSignInDto()));

		mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessTk))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(signUp.getEmail()))
				.andExpect(jsonPath("$.name").value(signUp.getName()))
				.andExpect(jsonPath("$.role").value("USER"))
				.andExpect(jsonPath("$.password").doesNotExist());
	}

	/**
	 * Sign-in sets the refresh token as cookie; exchanging it at /auth/access-tk
	 * returns a new access token that /me accepts as well.
	 */
	@Test
	void shouldIssueWorkingAccessTkFromRefreshCookie() throws Exception {
		signUp(AuthTestDataFactory.createSignUpDto());
		Cookie refreshCookie = signIn(AuthTestDataFactory.createSignInDto()).getResponse().getCookie("refresh_tk");

		MvcResult refreshed = mockMvc.perform(get("/auth/access-tk").cookie(refreshCookie))
				.andExpect(status().isOk())
				.andReturn();

		mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessTkOf(refreshed)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.email").value(AuthTestDataFactory.createSignUpDto().getEmail()));
	}

	/**
	 * A refresh cookie that is no valid token (tampered, forged, expired) is a
	 * client error: 401, not the catch-all 500.
	 */
	@Test
	void shouldRejectInvalidRefreshCookie() throws Exception {
		mockMvc.perform(get("/auth/access-tk").cookie(new Cookie("refresh_tk", "not-a-jwt")))
				.andExpect(status().isUnauthorized());
	}

	/**
	 * The stored role ends up in the token: an ADMIN may call the admin route, a
	 * regular USER gets 403. Sign-up cannot create admins, so the admin is put
	 * into the database directly, the way an operator would promote someone.
	 */
	@Test
	void shouldAllowAdminRouteOnlyForAdminAccounts() throws Exception {
		User admin = AuthTestDataFactory.createAdminUser();
		admin.setId(null); // let JPA generate it
		admin.setPassword(passwordEncoder.encode(AuthTestDataFactory.createAdminSignInDto().getPassword()));
		userRepository.save(admin);
		signUp(AuthTestDataFactory.createSignUpDto());

		String adminTk = accessTkOf(signIn(AuthTestDataFactory.createAdminSignInDto()));
		String userTk = accessTkOf(signIn(AuthTestDataFactory.createSignInDto()));

		mockMvc.perform(get("/admin/ping").header("Authorization", "Bearer " + adminTk))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$").value(containsString("pong")));
		mockMvc.perform(get("/admin/ping").header("Authorization", "Bearer " + userTk))
				.andExpect(status().isForbidden());
	}

	/**
	 * Self-registration never grants a role: a "role": "ADMIN" smuggled into the
	 * JSON is ignored and the account is a USER.
	 */
	@Test
	void shouldIgnoreRoleSentWithSignUp() throws Exception {
		String json = objectMapper.writeValueAsString(AuthTestDataFactory.createSignUpDto())
				.replaceFirst("\\{", "{\"role\":\"ADMIN\",");
		mockMvc.perform(post("/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(json))
				.andExpect(status().isCreated());

		String accessTk = accessTkOf(signIn(AuthTestDataFactory.createSignInDto()));

		mockMvc.perform(get("/me").header("Authorization", "Bearer " + accessTk))
				.andExpect(jsonPath("$.role").value("USER"));
		mockMvc.perform(get("/admin/ping").header("Authorization", "Bearer " + accessTk))
				.andExpect(status().isForbidden());
	}

	/**
	 * Emails are compared without case, so a capital letter at sign-in does not
	 * lock the user out. (Surrounding spaces are already rejected by @Email; the
	 * frontend trims them before sending.)
	 */
	@Test
	void shouldSignInWithDifferentlyCasedEmail() throws Exception {
		signUp(AuthTestDataFactory.createSignUpDto());

		SignInAccountRequestDto shouted = AuthTestDataFactory.createSignInDto();
		shouted.setEmail(shouted.getEmail().toUpperCase());

		signIn(shouted);
	}

	/**
	 * A second sign-up with the same email and phone is rejected with 400 and one
	 * field error each, because both are already stored in the database.
	 */
	@Test
	void shouldRejectSecondSignUpWithSameEmailAndPhone() throws Exception {
		signUp(AuthTestDataFactory.createSignUpDto());

		mockMvc.perform(post("/auth/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(AuthTestDataFactory.createSignUpDto())))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.errors[*].field").value(hasItems("email", "phone")));
	}

	/**
	 * The JWKS endpoint publishes the key that KeyConfig loaded from the key files,
	 * in the shape other services expect: one RSA key for signatures (RS256).
	 * {@code n} (modulus) and {@code e} (exponent) together are the public key;
	 * "AQAB" is the usual exponent 65537 in Base64url.
	 */
	@Test
	void shouldPublishPublicKeyAsJwks() throws Exception {
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

	/**
	 * Plays another microservice: it knows nothing but the published jwks.json,
	 * builds its own decoder from it and verifies a token Overex issued. This is
	 * the point of RS256: verification without a call back to Overex.
	 *
	 * <p>
	 * The decoder is deliberately built from the JSON and not taken from the
	 * {@code JwtDecoder} bean: the bean gets its key straight from KeyConfig, so it
	 * would still work if the published key were broken.
	 */
	@Test
	void shouldVerifyAccessTkWithPublishedJwks() throws Exception {
		SignUpAccountRequestDto signUp = AuthTestDataFactory.createSignUpDto();
		signUp(signUp);
		String accessTk = accessTkOf(signIn(AuthTestDataFactory.createSignInDto()));

		// decode() throws if the signature does not match the published key
		Jwt jwt = decoderFromJwks().decode(accessTk);

		assertEquals(signUp.getEmail(), jwt.getClaimAsString("email"));
	}

	/**
	 * The refresh token is signed with the same key, so its signature is valid
	 * too. Only the {@code type_tk} check stops a service from accepting it as an
	 * access token; every service that verifies Overex tokens needs it.
	 */
	@Test
	void shouldRejectRefreshTkWithPublishedJwks() throws Exception {
		signUp(AuthTestDataFactory.createSignUpDto());
		String refreshTk = signIn(AuthTestDataFactory.createSignInDto()).getResponse().getCookie("refresh_tk").getValue();

		assertThrows(JwtValidationException.class, () -> decoderFromJwks().decode(refreshTk));
	}

	/**
	 * Builds a decoder the way another service would: only from the jwks.json,
	 * with the same validators as SecurityConfig (expiry etc. plus "only access
	 * tokens").
	 */
	private JwtDecoder decoderFromJwks() throws Exception {
		String json = mockMvc.perform(get("/.well-known/jwks.json"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		RSAPublicKey publishedKey = JWKSet.parse(json).getKeys().get(0).toRSAKey().toRSAPublicKey();

		NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(publishedKey).build();
		decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
				JwtValidators.createDefault(),
				new JwtClaimValidator<String>("type_tk", TkName.ACCESS.name()::equals)));
		return decoder;
	}

	/** Signs up the given account and expects 201. */
	private void signUp(SignUpAccountRequestDto dto) throws Exception {
		mockMvc.perform(post("/auth/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isCreated());
	}

	/** Signs in, expects 200 and returns the whole result (body and cookies). */
	private MvcResult signIn(SignInAccountRequestDto dto) throws Exception {
		return mockMvc.perform(post("/auth/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(dto)))
				.andExpect(status().isOk())
				.andReturn();
	}

	/** Reads the access token from the JSON body of a sign-in or refresh response. */
	private static String accessTkOf(MvcResult result) throws Exception {
		return JsonPath.read(result.getResponse().getContentAsString(), "$.accessTk");
	}
}
