package com.overex.backend.slice.auth;

import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.overex.backend.auth.AuthController;
import com.overex.backend.auth.AuthService;
import com.overex.backend.auth.JwtService;
import com.overex.backend.auth.dto.requests.SignInAccountRequestDto;
import com.overex.backend.auth.dto.requests.SignUpAccountRequestDto;
import com.overex.backend.common.entities.User;
import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.utils.TestDataFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer tests for {@link AuthController}.
 *
 * <p>
 * Drives the three public endpoints through MockMvc with the service and JWT
 * layers mocked, asserting status codes, the {@code AuthResponseDto} shape and
 * the error envelopes produced by the global exception handler.
 */
@WebMvcTest(AuthController.class)
// The slice only auto-configures Jackson 3; this test needs a Jackson 2
// ObjectMapper to turn the request DTOs into JSON.
@Import(ObjectMapper.class)
public class AuthControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private JwtService jwtService;

	private final User user = TestDataFactory.createDefaultUser();

	/** A successful sign-up returns 201 with an access token and its lifetime. */
	@Test
	void shouldSignUpAccount() throws Exception {
		SignUpAccountRequestDto dto = TestDataFactory.createSignUpDto();
		String jsonRequest = objectMapper.writeValueAsString(dto);

		when(authService.signUp(dto)).thenReturn(user);
		when(jwtService.createAccessTk(any())).thenReturn("mock-access-token");
		when(jwtService.createRefreshTk(any())).thenReturn("mock-refresh-token");

		mockMvc.perform(post("/auth/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.accessTk").isString())
				.andExpect(jsonPath("$.accessTk").isNotEmpty())
				.andExpect(jsonPath("$.expiresIn").isNumber())
				.andExpect(jsonPath("$.expiresIn").value(900)); // seconds = the 15-minute access-token lifetime
	}

	/** Only the email is taken: 400 with exactly one field error for {@code email}. */
	@Test
	void shouldNotSignUpAccountWithSingleWrongField() throws Exception {
		SignUpAccountRequestDto dto = TestDataFactory.createSignUpDto();
		String jsonRequest = objectMapper.writeValueAsString(dto);
		List<ApiError.FieldError> errors = List.of(
				ApiError.FieldError.builder().field("email").message("The used email is already taken.").build());
		when(authService.signUp(dto)).thenThrow(new OwnException(errors));

		mockMvc.perform(post("/auth/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors[*].field").value(hasItems("email")));
	}

	/** Taken email and phone come back as 400 with one field error each. */
	@Test
	void shouldNotSignUpAccountWithMultipleWrongFields() throws Exception {
		SignUpAccountRequestDto dto = TestDataFactory.createSignUpDto();
		String jsonRequest = objectMapper.writeValueAsString(dto);
		List<ApiError.FieldError> errors = List.of(
				ApiError.FieldError.builder().field("email").message("The used email is already taken.").build(),
				ApiError.FieldError.builder().field("phone").message("The used phone is already taken.").build());
		when(authService.signUp(dto)).thenThrow(new OwnException(errors));

		mockMvc.perform(post("/auth/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors").isArray())
				.andExpect(jsonPath("$.errors[*].field").value(hasItems("email", "phone")));
	}

	/**
	 * Bean validation rejects blank or malformed fields with 400 in the same error
	 * shape.
	 */
	@Test
	void shouldRejectInvalidSignUpPayload() throws Exception {
		// Missing/blank fields must be rejected by bean validation and surfaced
		// in the same ApiError shape (errors[].field) the frontend already maps.
		String invalidJson = "{\"name\":\"\",\"email\":\"not-an-email\",\"phone\":\"\",\"password\":\"123\"}";

		mockMvc.perform(post("/auth/sign-up")
				.contentType(MediaType.APPLICATION_JSON)
				.content(invalidJson))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors").isArray());
	}

	/** A successful sign-in returns 200 with an access token and its lifetime. */
	@Test
	void shouldSignIn() throws Exception {
		SignInAccountRequestDto dto = TestDataFactory.createSignInDto();
		String jsonRequest = objectMapper.writeValueAsString(dto);
		when(authService.authenticate(any(), any())).thenReturn(user);
		when(jwtService.createAccessTk(any())).thenReturn("mock-access-token");
		when(jwtService.createRefreshTk(any())).thenReturn("mock-refresh-token");

		mockMvc.perform(post("/auth/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessTk").isString())
				.andExpect(jsonPath("$.accessTk").isNotEmpty())
				.andExpect(jsonPath("$.expiresIn").isNumber())
				.andExpect(jsonPath("$.expiresIn").value(900));
	}

	/** Wrong credentials return 401 without field errors. */
	@Test
	void shouldNotSignIn() throws Exception {
		SignInAccountRequestDto dto = TestDataFactory.createSignInDto();
		String jsonRequest = objectMapper.writeValueAsString(dto);

		when(authService.authenticate(any(), any())).thenThrow(new BadCredentialsException("Invalid credentials"));

		mockMvc.perform(post("/auth/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.status").value(401))
				// no field errors: the response must not reveal whether the email exists
				.andExpect(jsonPath("$.errors").doesNotExist());
	}

	/**
	 * An unexpected exception from the service is caught by the catch-all handler
	 * in the {@code GlobalExceptionHandler} and becomes a 500 in the common error
	 * envelope; the application keeps running.
	 */
	@Test
	void shouldNotSignInAndTriggerLastResortExceptionHandler() throws Exception {
		SignInAccountRequestDto dto = TestDataFactory.createSignInDto();
		String jsonRequest = objectMapper.writeValueAsString(dto);

		when(authService.authenticate(any(), any()))
				.thenThrow(new RuntimeException("Triggered last resort exception handler"));

		mockMvc.perform(post("/auth/sign-in")
				.contentType(MediaType.APPLICATION_JSON)
				.content(jsonRequest))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.error").value("Internal Server Error"))
				.andExpect(jsonPath("$.path").value("/auth/sign-in"));
	}

	/** A valid refresh cookie is exchanged for a new access token. */
	@Test
	void shouldCreateAccessTk() throws Exception {
		when(jwtService.extractRefreshTk(any())).thenReturn("Token");
		when(jwtService.validateRefreshTk(any())).thenReturn(user);
		when(jwtService.createAccessTk(any())).thenReturn("mock-access-token");

		mockMvc.perform(get("/auth/access-tk"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.accessTk").isString())
				.andExpect(jsonPath("$.accessTk").isNotEmpty())
				.andExpect(jsonPath("$.expiresIn").isNumber())
				.andExpect(jsonPath("$.expiresIn").value(900));
	}

	/** An invalid refresh cookie (wrong type, user gone, ...) returns 401. */
	@Test
	void shouldRejectRefreshTk() throws Exception {
		when(jwtService.extractRefreshTk(any())).thenReturn("Token");
		when(jwtService.validateRefreshTk(any())).thenThrow(new BadCredentialsException("Invalid refresh token"));

		mockMvc.perform(get("/auth/access-tk"))
				.andExpect(status().isUnauthorized());
	}

	/** Without a refresh cookie the request is rejected with 401. */
	@Test
	void shouldNotCreateAccessTkWithNullRefreshTk() throws Exception {
		when(jwtService.extractRefreshTk(any())).thenReturn(null);

		mockMvc.perform(get("/auth/access-tk"))
				.andExpect(status().isUnauthorized());
	}

	/** A refresh cookie that is present but empty is treated like a missing one: 401. */
	@Test
	void shouldNotCreateAccessTkWithEmptyRefreshTk() throws Exception {
		when(jwtService.extractRefreshTk(any())).thenReturn("");

		mockMvc.perform(get("/auth/access-tk"))
				.andExpect(status().isUnauthorized());
	}
}
