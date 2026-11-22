package com.overex.backend.auth.unit.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import com.overex.backend.auth.config.RsaKeyFiles;
import com.overex.backend.auth.utils.TestRsaKeys;

/**
 * Unit tests for {@link RsaKeyFiles}, which {@code KeyConfig} uses to read the
 * key files.
 *
 * <p>
 * No Spring context: the PEM text is built in memory from the key pair of
 * {@link TestRsaKeys} and handed over as a {@link ByteArrayResource}; only the
 * round trip test writes real files, into a temporary folder.
 * The happy path compares the parsed key with the original, so it checks that
 * the right key comes out and not only that the code runs. The error paths
 * check that a bad key source stops the start-up with a clear message.
 */
public class RsaKeyFilesTest {

	/** The PEM private key is parsed into exactly the key it was built from. */
	@Test
	void shouldReadPrivateKey() {
		// Arrange
		Resource pem = pem("PRIVATE", TestRsaKeys.privateKey().getEncoded());

		// Act
		RSAPrivateKey key = RsaKeyFiles.readPrivateKey(pem);

		// Assert
		assertArrayEquals(TestRsaKeys.privateKey().getEncoded(), key.getEncoded());
	}

	/** The PEM public key is parsed into exactly the key it was built from. */
	@Test
	void shouldReadPublicKey() {
		// Arrange
		Resource pem = pem("PUBLIC", TestRsaKeys.publicKey().getEncoded());

		// Act
		RSAPublicKey key = RsaKeyFiles.readPublicKey(pem);

		// Assert
		assertArrayEquals(TestRsaKeys.publicKey().getEncoded(), key.getEncoded());
	}

	/** Text that is not Base64 is rejected with a clear start-up error. */
	@Test
	void shouldRejectPrivateKeyThatIsNotBase64() {
		// Act + Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> RsaKeyFiles.readPrivateKey(text("this is no key!!")));

		assertTrue(ex.getMessage().startsWith("Cannot read the JWT private key"));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
	}

	/** Valid Base64 that is not a key structure is rejected. */
	@Test
	void shouldRejectPrivateKeyWithInvalidKeyBytes() {
		// Act + Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> RsaKeyFiles.readPrivateKey(pem("PRIVATE", new byte[] { 1, 2, 3 })));

		assertTrue(ex.getMessage().startsWith("Cannot read the JWT private key"));
		assertInstanceOf(GeneralSecurityException.class, ex.getCause());
	}

	/** A key file that cannot be read (missing, no permission) is rejected. */
	@Test
	void shouldRejectUnreadablePrivateKey() {
		// Act + Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> RsaKeyFiles.readPrivateKey(unreadable()));

		assertTrue(ex.getMessage().startsWith("Cannot read the JWT private key"));
		assertInstanceOf(IOException.class, ex.getCause());
	}

	/** Text that is not Base64 is rejected with a clear start-up error. */
	@Test
	void shouldRejectPublicKeyThatIsNotBase64() {
		// Act + Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> RsaKeyFiles.readPublicKey(text("this is no key!!")));

		assertTrue(ex.getMessage().startsWith("Cannot read the JWT public key"));
		assertInstanceOf(IllegalArgumentException.class, ex.getCause());
	}

	/** Valid Base64 that is not a key structure is rejected. */
	@Test
	void shouldRejectPublicKeyWithInvalidKeyBytes() {
		// Act + Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> RsaKeyFiles.readPublicKey(pem("PUBLIC", new byte[] { 1, 2, 3 })));

		assertTrue(ex.getMessage().startsWith("Cannot read the JWT public key"));
		assertInstanceOf(GeneralSecurityException.class, ex.getCause());
	}

	/** A key file that cannot be read (missing, no permission) is rejected. */
	@Test
	void shouldRejectUnreadablePublicKey() {
		// Act + Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class,
				() -> RsaKeyFiles.readPublicKey(unreadable()));

		assertTrue(ex.getMessage().startsWith("Cannot read the JWT public key"));
		assertInstanceOf(IOException.class, ex.getCause());
	}

	/** The error message names the resource, so the broken path is easy to find. */
	@Test
	void shouldNameTheResourceInTheErrorMessage() {
		// Arrange
		Resource broken = new ByteArrayResource("x".getBytes(StandardCharsets.UTF_8), "classpath:keys/public.pem");

		// Act + Assert
		IllegalStateException ex = assertThrows(IllegalStateException.class, () -> RsaKeyFiles.readPublicKey(broken));

		assertTrue(ex.getMessage().contains("classpath:keys/public.pem"));
	}

	/**
	 * What {@link RsaKeyFiles#write} puts on disk is read back as the same keys,
	 * so the startup generator and {@code KeyConfig} agree on the file format.
	 */
	@Test
	void shouldReadBackTheKeysItWrote(@TempDir Path dir) {
		// Arrange
		Path privateFile = dir.resolve("keys/private.pem");
		Path publicFile = dir.resolve("keys/public.pem");

		// Act
		RsaKeyFiles.write(TestRsaKeys.PAIR, privateFile, publicFile);

		// Assert
		assertArrayEquals(TestRsaKeys.privateKey().getEncoded(),
				RsaKeyFiles.readPrivateKey(new FileSystemResource(privateFile)).getEncoded());
		assertArrayEquals(TestRsaKeys.publicKey().getEncoded(),
				RsaKeyFiles.readPublicKey(new FileSystemResource(publicFile)).getEncoded());
	}

	/** PEM text from {@link RsaKeyFiles#pem(String, byte[])} as an in-memory {@link Resource}. */
	private static Resource pem(String label, byte[] encodedKey) {
		return text(RsaKeyFiles.pem(label, encodedKey));
	}

	/** Wraps plain text as an in-memory {@link Resource}. */
	private static Resource text(String content) {
		return new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8));
	}

	/** A resource whose stream cannot be opened, like a missing key file. */
	private static Resource unreadable() {
		return new ByteArrayResource(new byte[0]) {
			@Override
			public InputStream getInputStream() throws IOException {
				throw new IOException("file not found");
			}
		};
	}
}
