package com.overex.backend.auth.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import org.springframework.core.io.Resource;

/**
 * The one place that knows the PEM format of the JWT keys, in both directions:
 * it reads the key files ({@link KeyConfig} turns them into beans) and creates
 * and writes them ({@link DevKeyGenerator} on startup, {@code TestRsaKeys} in the
 * tests). Private keys are PKCS#8, public keys X.509.
 *
 * <p>
 * Plain static helpers, not a bean: {@link DevKeyGenerator} runs before the
 * Spring context (and therefore any bean) exists.
 */
public final class RsaKeyFiles {

	private RsaKeyFiles() {
	}

	/**
	 * Reads a private key from a PKCS#8 PEM file.
	 *
	 * @throws IllegalStateException when the file is missing or is not a valid PKCS#8 RSA key
	 */
	public static RSAPrivateKey readPrivateKey(Resource location) {
		try {
			byte[] keyBytes = decodePem(location, "PRIVATE");
			return (RSAPrivateKey) KeyFactory.getInstance("RSA")
					.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
		} catch (IOException | GeneralSecurityException | IllegalArgumentException ex) {
			throw new IllegalStateException("Cannot read the JWT private key from " + location, ex);
		}
	}

	/**
	 * Reads a public key from an X.509 PEM file.
	 *
	 * @throws IllegalStateException when the file is missing or is not a valid X.509 RSA key
	 */
	public static RSAPublicKey readPublicKey(Resource location) {
		try {
			byte[] keyBytes = decodePem(location, "PUBLIC");
			return (RSAPublicKey) KeyFactory.getInstance("RSA")
					.generatePublic(new X509EncodedKeySpec(keyBytes));
		} catch (IOException | GeneralSecurityException | IllegalArgumentException ex) {
			throw new IllegalStateException("Cannot read the JWT public key from " + location, ex);
		}
	}

	/**
	 * Strips the {@code BEGIN/END <label> KEY} frame and all whitespace from the
	 * file and decodes the Base64 body into the raw key bytes.
	 *
	 * @throws IllegalArgumentException when the body is not valid Base64
	 */
	private static byte[] decodePem(Resource location, String label) throws IOException {
		String data = new String(location.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

		// Strings are immutable: replace() returns a NEW string, so the result must be reassigned.
		String base64 = data
				.replace("-----BEGIN " + label + " KEY-----", "")
				.replace("-----END " + label + " KEY-----", "")
				.replaceAll("\\s", ""); // line breaks and spaces are not part of the Base64 body

		return Base64.getDecoder().decode(base64);
	}

	/** Generates a fresh 2048-bit RSA key pair. */
	public static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
			generator.initialize(2048);
			return generator.generateKeyPair();
		} catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException("RSA is not available in this JVM", ex);
		}
	}

	/**
	 * Builds PEM text from an encoded key: a {@code BEGIN/END <label> KEY} frame
	 * around the Base64 body, wrapped at 64 characters per line.
	 *
	 * @param label      {@code "PRIVATE"} or {@code "PUBLIC"}
	 * @param encodedKey {@code Key#getEncoded()} (PKCS#8 for private, X.509 for
	 *                   public keys, the formats {@link KeyConfig} reads)
	 */
	public static String pem(String label, byte[] encodedKey) {
		String body = Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8)).encodeToString(encodedKey);
		return "-----BEGIN " + label + " KEY-----\n" + body + "\n-----END " + label + " KEY-----\n";
	}

	/**
	 * Writes the pair as two PEM files, creating missing parent folders.
	 *
	 * <p>
	 * The files keep the default permissions on purpose: dev keys are mounted into
	 * the backend container, where the app runs as another user ({@code overex})
	 * that must be able to read them. Production keys are not written here; they
	 * come from a secret whose permissions the platform sets.
	 */
	public static void write(KeyPair pair, Path privateFile, Path publicFile) {
		writeString(privateFile, pem("PRIVATE", pair.getPrivate().getEncoded()));
		writeString(publicFile, pem("PUBLIC", pair.getPublic().getEncoded()));
	}

	private static void writeString(Path file, String content) {
		try {
			if (file.getParent() != null) {
				Files.createDirectories(file.getParent());
			}
			Files.writeString(file, content);
		} catch (IOException ex) {
			throw new UncheckedIOException("Cannot write the JWT key file " + file, ex);
		}
	}
}
