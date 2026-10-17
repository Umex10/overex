package com.overex.backend.auth.unit.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.bootstrap.DefaultBootstrapContext;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mock.env.MockEnvironment;

import com.overex.backend.auth.config.DevKeyGenerator;
import com.overex.backend.auth.config.RsaKeyFiles;

/**
 * Unit tests for {@link DevKeyGenerator}.
 *
 * <p>
 * No Spring context: a {@link MockEnvironment} stands in for the properties
 * Spring would have read (application.properties, environment variables), and
 * the event is created by hand, the same event Spring sends on startup once the
 * properties are read. The key locations point into a {@link TempDir}, so the
 * real {@code apps/backend/keys} folder is never touched.
 */
public class DevKeyGeneratorTest {

	/** Fresh empty folder per test, deleted by JUnit afterwards. */
	@TempDir
	Path dir;

	private final DevKeyGenerator generator = new DevKeyGenerator();

	/**
	 * The normal dev start after a clone: switch on, no key files yet. Both files
	 * are created (including the missing "keys" folder) and form a real pair.
	 */
	@Test
	void shouldCreateMatchingKeyPairWhenBothFilesAreMissing() {
		Path privateFile = dir.resolve("keys/private.pem");
		Path publicFile = dir.resolve("keys/public.pem");

		start(environment(true, privateFile, publicFile));

		assertTrue(Files.exists(privateFile));
		assertTrue(Files.exists(publicFile));
		// The files are readable the way KeyConfig reads them, and belong together:
		// an RSA private key carries its public modulus, which must equal the
		// modulus of the public key.
		RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) RsaKeyFiles.readPrivateKey(new FileSystemResource(privateFile));
		RSAPublicKey publicKey = RsaKeyFiles.readPublicKey(new FileSystemResource(publicFile));
		assertEquals(publicKey.getModulus(), privateKey.getModulus());
	}

	/**
	 * The integration test writes its locations in URI form ("file:///tmp/...");
	 * they are translated into paths just like the short "file:./keys/..." form.
	 */
	@Test
	void shouldUnderstandFileLocationsInUriForm() {
		Path privateFile = dir.resolve("private.pem");
		Path publicFile = dir.resolve("public.pem");
		MockEnvironment env = new MockEnvironment()
				.withProperty("jwt.generate-missing-keys", "true")
				.withProperty("jwt.private-key-location", privateFile.toUri().toString())
				.withProperty("jwt.public-key-location", publicFile.toUri().toString());

		start(env);

		assertTrue(Files.exists(privateFile));
		assertTrue(Files.exists(publicFile));
	}

	/** Existing keys are never replaced: that would invalidate every issued token. */
	@Test
	void shouldKeepExistingKeys() throws IOException {
		Path privateFile = dir.resolve("private.pem");
		Path publicFile = dir.resolve("public.pem");
		Files.writeString(privateFile, "existing private key");
		Files.writeString(publicFile, "existing public key");

		start(environment(true, privateFile, publicFile));

		assertEquals("existing private key", Files.readString(privateFile));
		assertEquals("existing public key", Files.readString(publicFile));
	}

	/**
	 * Only one file present: nothing is created. A new public key would not match
	 * the existing private key; KeyConfig then fails with a clear error instead.
	 */
	@Test
	void shouldNotCompleteAHalfPair() throws IOException {
		Path privateFile = dir.resolve("private.pem");
		Path publicFile = dir.resolve("public.pem");
		Files.writeString(privateFile, "existing private key");

		start(environment(true, privateFile, publicFile));

		assertFalse(Files.exists(publicFile));
		assertEquals("existing private key", Files.readString(privateFile));
	}

	/** Switch off (as in the Docker image): missing keys stay missing. */
	@Test
	void shouldDoNothingWhenSwitchedOff() {
		Path privateFile = dir.resolve("private.pem");
		Path publicFile = dir.resolve("public.pem");

		start(environment(false, privateFile, publicFile));

		assertFalse(Files.exists(privateFile));
		assertFalse(Files.exists(publicFile));
	}

	/**
	 * Switch not configured anywhere: the code default "false" applies, so when in
	 * doubt nothing is generated.
	 */
	@Test
	void shouldDoNothingWhenSwitchIsNotConfigured() {
		Path privateFile = dir.resolve("private.pem");
		Path publicFile = dir.resolve("public.pem");
		MockEnvironment env = new MockEnvironment()
				.withProperty("jwt.private-key-location", "file:" + privateFile)
				.withProperty("jwt.public-key-location", "file:" + publicFile);

		start(env);

		assertFalse(Files.exists(privateFile));
		assertFalse(Files.exists(publicFile));
	}

	/**
	 * Locations that are no files (here classpath:) cannot be written to; the
	 * generator steps aside without an error and leaves the rest to KeyConfig.
	 */
	@Test
	void shouldIgnoreLocationsThatAreNoFiles() {
		MockEnvironment env = new MockEnvironment()
				.withProperty("jwt.generate-missing-keys", "true")
				.withProperty("jwt.private-key-location", "classpath:keys/private.pem")
				.withProperty("jwt.public-key-location", "classpath:keys/public.pem");

		start(env);

		assertFalse(Files.exists(dir.resolve("keys")));
	}

	/** Without a key location the app cannot run at all, so startup stops right here. */
	@Test
	void shouldFailWhenKeyLocationIsMissing() {
		MockEnvironment env = new MockEnvironment()
				.withProperty("jwt.generate-missing-keys", "true");

		assertThrows(IllegalStateException.class, () -> start(env));
	}

	/** An environment with the switch and both locations in the short "file:" form. */
	private static MockEnvironment environment(boolean generate, Path privateFile, Path publicFile) {
		return new MockEnvironment()
				.withProperty("jwt.generate-missing-keys", String.valueOf(generate))
				.withProperty("jwt.private-key-location", "file:" + privateFile)
				.withProperty("jwt.public-key-location", "file:" + publicFile);
	}

	/**
	 * Hands the generator the event Spring sends once the properties are read
	 * (step 2 of the startup), with the given environment inside.
	 */
	private void start(MockEnvironment env) {
		generator.onApplicationEvent(new ApplicationEnvironmentPreparedEvent(
				new DefaultBootstrapContext(), new SpringApplication(), new String[0], env));
	}
}
