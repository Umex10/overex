package com.overex.backend.common.entities;

/**
 * Application-level user roles.
 *
 * <p>
 * Persisted as a string on the {@link User} entity, written into the access
 * token's {@code roles} claim and mapped to the Spring Security authority
 * {@code ROLE_<NAME>}, so {@code @PreAuthorize("hasRole('ADMIN')")} works.
 *
 * <p>
 * Sign-up always creates a {@link #USER}; nobody can pick a role for
 * themselves. Admins are promoted deliberately, e.g. directly in the database.
 *
 * <p>
 * <b>Customising for your project:</b> replace or extend these two generic
 * roles (e.g. {@code OWNER}, {@code DRIVER}); they flow through to the token
 * and the {@code @PreAuthorize} checks automatically.
 */
public enum Role {
	USER,
	ADMIN
}
