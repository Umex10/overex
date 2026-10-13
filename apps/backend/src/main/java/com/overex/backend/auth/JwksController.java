package com.overex.backend.auth;

import java.security.interfaces.RSAPublicKey;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Publishes the public signing key as a JSON Web Key Set (JWKS).
 *
 * <p>Other services that need to verify Overex's access tokens, without being
 * able to create them, point their
 * {@code spring.security.oauth2.resourceserver.jwt.jwk-set-uri} at this route.
 * It is public by design: a public key is not a secret.
 *
 * @see com.overex.backend.auth.config.KeyConfig
 */
@Tag(name = "JWKS", description = "Public verification key for access tokens — no authentication required")
@RestController
@RequiredArgsConstructor
public class JwksController {

	private final RSAPublicKey publicKey;

	/**
	 * Returns the public key as a JWK set.
	 *
	 * @return the JWKS JSON ({@code {"keys":[{...}]}}); never contains private key material
	 */
	@Operation(summary = "Get the JSON Web Key Set", description = "The public key needed to verify access tokens (RS256).")
	@GetMapping("/.well-known/jwks.json")
	public Map<String, Object> jwks() {
		RSAKey jwk = new RSAKey.Builder(publicKey)
				.keyUse(KeyUse.SIGNATURE)     // "use": "sig" -> this key verifies signatures
				.algorithm(JWSAlgorithm.RS256) // "alg": "RS256"
				.build();

		return new JWKSet(jwk).toJSONObject(); // public parts only (n, e), serialised to JSON by Spring
	}
}
