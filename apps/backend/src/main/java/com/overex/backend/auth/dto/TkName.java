package com.overex.backend.auth.dto;

/**
 * Discriminator for the two JWT flavours issued by the auth module.
 *
 * <p>Stored as the {@code type_tk} claim inside every token so the receiver can
 * tell access tokens (short-lived, sent in the {@code Authorization} header)
 * apart from refresh tokens (long-lived, stored in the {@code refresh_tk}
 * HTTP-only cookie).
 */
public enum TkName {
	REFRESH,
	ACCESS
}
