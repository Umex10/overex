package com.overex.backend;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.overex.backend.auth.utils.TestRsaKeys;

/**
 * Smoke test: the whole application context starts with the "test" profile.
 *
 * <p>
 * Fails when a bean can't be created or wired, a property is missing or the
 * JPA mapping doesn't fit the schema, before any feature test has to find out.
 * The feature flows themselves are covered by the integration tests of each
 * feature (e.g. {@code AuthIntegrationTest}).
 *
 * <p>
 * The real {@code KeyConfig} needs key files, so the key pair of
 * {@link TestRsaKeys} is written into a temporary folder first, the same way
 * {@code AuthIntegrationTest} does it.
 */
@SpringBootTest
@ActiveProfiles("test")
class BackendApplicationTests {

	/** Temporary folder for the generated key files, created before the context starts. */
	@TempDir
	static Path keyDir;

	/** Writes the key files and points the key-location properties at them. */
	@DynamicPropertySource
	static void keyLocations(DynamicPropertyRegistry registry) {
		TestRsaKeys.writePemFiles(keyDir);
		registry.add("jwt.private-key-location", () -> keyDir.resolve("private.pem").toUri().toString());
		registry.add("jwt.public-key-location", () -> keyDir.resolve("public.pem").toUri().toString());
	}

	/** Passes as soon as the context has started; JUnit fails it if the start throws. */
	@Test
	void contextLoads() {
	}
}
