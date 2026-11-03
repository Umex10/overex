/**
 * Shape of the JSON payload returned by every successful auth endpoint.
 *
 * Mirrors the backend's `AuthResponseDto`: the short-lived access token plus
 * its remaining lifetime in seconds. The refresh token travels separately as
 * an HTTP-only cookie and is therefore not part of this interface.
 */
export interface AuthResponseDto {
  accessTk: string;
  expiresIn: number;
}
