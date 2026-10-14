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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.http.MediaType;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class TkFactory {

    private TkFactory() {
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

    public static String signUpAndReturnAccessTk(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        SignUpAccountRequestDto signUpAccountRequestDto = AuthTestDataFactory.createSignUpDto();
        MvcResult mvcResult = mockMvc.perform(post("/auth/sign-up")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signUpAccountRequestDto)))
                .andExpect(status().isCreated()).andReturn();
        return accessTkOf(mvcResult);
    }

    /** Signs in, expects 200 and returns the whole result (body and cookies). */
    public static String signInAndReturnAccessTk(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        SignInAccountRequestDto signInAccountRequestDto = AuthTestDataFactory.createSignInDto();
        MvcResult mvcResult = mockMvc.perform(post("/auth/sign-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(signInAccountRequestDto)))
                .andExpect(status().isOk())
                .andReturn();

        return accessTkOf(mvcResult);
    }

    /**
     * Reads the access token from the JSON body of a sign-in or refresh response.
     */
    public static String accessTkOf(MvcResult result) throws Exception {
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessTk");
    }

}
