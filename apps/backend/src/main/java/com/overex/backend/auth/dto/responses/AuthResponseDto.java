package com.overex.backend.auth.dto.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response payload returned by every successful auth endpoint.
 *
 * <p>Exposes only the short-lived access token (the refresh token travels in a
 * separate HTTP-only cookie) and its remaining lifetime in seconds so the
 * client can schedule a silent refresh before expiry.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {

	/** The signed, short-lived JWT the client sends as {@code Authorization: Bearer ...}. */
	private String accessTk;

	/** Remaining access-token lifetime in seconds (900 = 15 minutes). */
	private Long expiresIn;
}
