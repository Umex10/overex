package com.overex.backend.auth.dto.responses;

import lombok.Builder;

/**
 * Response payload returned by every successful auth endpoint.
 *
 * <p>Exposes only the short-lived access token (the refresh token travels in a
 * separate HTTP-only cookie) and its remaining lifetime in seconds so the
 * client can schedule a silent refresh before expiry.
 *
 * @param accessTk  the signed, short-lived JWT the client sends as
 *                  {@code Authorization: Bearer ...}
 * @param expiresIn remaining access-token lifetime in seconds (900 = 15 minutes)
 */
@Builder
public record AuthResponseDto(String accessTk, Long expiresIn) {
}
