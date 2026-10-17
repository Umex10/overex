package com.overex.backend.auth.utils;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import com.overex.backend.auth.dto.requests.SignInAccountRequestDto;
import com.overex.backend.auth.dto.requests.SignUpAccountRequestDto;
import com.overex.backend.common.entities.User;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

/**
 * Auth actions for MockMvc tests: signs up/in through the real endpoints or
 * prepares an authenticated request. The test data itself comes from
 * {@link AuthTestDataFactory}.
 */
public final class AuthTestClient {

    private AuthTestClient() {
    }

    /**
     * Simulates a request that already passed token verification: puts a JWT
     * (subject = user id) with the user's {@code ROLE_} authority into the
     * security context.
     */
    public static RequestPostProcessor authenticatedAs(User user) {
        return jwt()
                .jwt(token -> token.subject(user.getId().toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    /** Signs up the given account, expects 201 and returns the whole result. */
    public static MvcResult signUp(MockMvc mockMvc, ObjectMapper objectMapper, SignUpAccountRequestDto dto)
            throws Exception {
        return mockMvc.perform(post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    /** Signs in, expects 200 and returns the whole result (body and cookies). */
    public static MvcResult signIn(MockMvc mockMvc, ObjectMapper objectMapper, SignInAccountRequestDto dto)
            throws Exception {
        return mockMvc.perform(post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andReturn();
    }

    /** Signs up the default user and returns the access token of the sign-up response. */
    public static String signUpAndReturnAccessTk(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return accessTkOf(signUp(mockMvc, objectMapper, AuthTestDataFactory.createSignUpDto()));
    }

    /** Signs in the default user and returns the access token. */
    public static String signInAndReturnAccessTk(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return accessTkOf(signIn(mockMvc, objectMapper, AuthTestDataFactory.createSignInDto()));
    }

    /**
     * Reads the access token from the JSON body of a sign-in or refresh response.
     */
    public static String accessTkOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessTk");
    }

}
