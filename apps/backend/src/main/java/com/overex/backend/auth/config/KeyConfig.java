package com.overex.backend.auth.config;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

/**
 * Turns the JWT key files into beans. Reading and parsing the PEM files is done
 * by {@link RsaKeyFiles}; this class only connects it to Spring: it takes the
 * locations from {@code jwt.private-key-location} / {@code jwt.public-key-location}
 * and publishes the keys for {@code JwtService}, the resource server and the
 * JWKS endpoint.
 */
@Configuration
public class KeyConfig {

    /**
     * The private signing key.
     *
     * @return the RSA private key used to sign access and refresh tokens
     * @throws IllegalStateException when the file is missing or is not a valid PKCS#8 RSA key
     */
    @Bean
    public RSAPrivateKey privateKey(@Value("${jwt.private-key-location}") Resource location) {
        return RsaKeyFiles.readPrivateKey(location);
    }

    /**
     * The public verification key.
     *
     * @return the RSA public key used to verify token signatures
     * @throws IllegalStateException when the file is missing or is not a valid X.509 RSA key
     */
    @Bean
    public RSAPublicKey publicKey(@Value("${jwt.public-key-location}") Resource location) {
        return RsaKeyFiles.readPublicKey(location);
    }
}
