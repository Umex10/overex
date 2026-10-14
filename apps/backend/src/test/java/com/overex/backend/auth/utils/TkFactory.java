package com.overex.backend.auth.utils;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.overex.backend.common.entities.User;

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

}
