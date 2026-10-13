package com.overex.backend.auth.config;

import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.util.ResourceUtils;

/**
 * Generates the JWT key pair on startup when neither key file exists yet, so the
 * service runs straight after a clone without creating keys by hand.
 *
 * <p>
 * It listens to {@link ApplicationEnvironmentPreparedEvent}: the properties are
 * already read (so the key locations are known), but no bean exists yet. A
 * {@code CommandLineRunner} would be too late, because {@link KeyConfig} reads
 * the files while the context is built.
 *
 * <p>
 * Rules:
 * <ul>
 * <li>Both files exist: nothing happens.</li>
 * <li>Neither file exists: a new 2048-bit RSA pair is written to both
 * locations.</li>
 * <li>Only one exists: nothing happens, {@link KeyConfig} then fails loudly. A
 * new private key would not match the existing public key (or the other way
 * round).</li>
 * <li>{@code jwt.generate-missing-keys=false} (the Docker image sets it):
 * nothing happens, missing keys are an error in production.</li>
 * </ul>
 *
 * <p>
 * Registered in {@code BackendApplication#main}, so it does not run in tests
 * ({@code @SpringBootTest} does not call {@code main}); they bring their own
 * keys.
 */
public class DevKeyGenerator implements ApplicationListener<ApplicationEnvironmentPreparedEvent> {

	private static final Logger log = LoggerFactory.getLogger(DevKeyGenerator.class);

	@Override
	public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
		// The environment holds every setting Spring has read so far:
		// application.properties, environment variables, command-line arguments.
		ConfigurableEnvironment env = event.getEnvironment();

		// The switch: may we create keys at all? "false" (the code default) is used
		// only if the property is not defined anywhere, so when in doubt: don't.
		if (!env.getProperty("jwt.generate-missing-keys", Boolean.class, false)) {
			return;
		}

		// The same locations KeyConfig reads later, with ${...} placeholders already
		// resolved, e.g. "file:./keys/private.pem". "Required": a missing property
		// throws right away, because the app cannot run without these locations.
		Path privateKey = toPath(env.getRequiredProperty("jwt.private-key-location"));
		Path publicKey = toPath(env.getRequiredProperty("jwt.public-key-location"));
		if (privateKey == null || publicKey == null) {
			return; // not a file (e.g. classpath: or base64:), nothing we could write to
		}

		// Only when BOTH files are missing: a new key next to an existing one
		// would not belong to the same pair.
		if (Files.exists(privateKey) || Files.exists(publicKey)) {
			return;
		}

		RsaKeyFiles.write(RsaKeyFiles.generateKeyPair(), privateKey, publicKey);
		// A warning, not an info: seen in production, it means the keys are misconfigured.
		log.warn("No JWT keys found, generated a new development key pair: {} and {}",
				privateKey.toAbsolutePath(), publicKey.toAbsolutePath());
	}

	/**
	 * Translates a Spring location into a Java path: from
	 * {@code "file:./keys/private.pem"} (Spring's way of saying "a file on disk")
	 * to {@code ./keys/private.pem}, which Java's {@link Files} methods
	 * understand.
	 *
	 * <p>
	 * A relative path like {@code ./keys} is resolved against the folder the app
	 * is started from (with {@code ./mvnw spring-boot:run}: {@code apps/backend}).
	 *
	 * @return the path, or {@code null} when the location is not a file (e.g.
	 *         {@code classpath:} or {@code base64:}) and there is nothing to write to
	 */
	private static Path toPath(String location) {
		// "file:" is Spring's prefix for the file system; anything else is no file.
		if (!location.startsWith(ResourceUtils.FILE_URL_PREFIX)) {
			return null;
		}
		try {
			// Strips "file:" and understands both the short form "file:./keys/x.pem"
			// and the URI form "file:///tmp/keys/x.pem" (used by the integration test).
			return ResourceUtils.getFile(location).toPath();
		} catch (FileNotFoundException ex) {
			// Despite its name, this does NOT mean the file is missing (that is not
			// checked here); it means the location cannot be turned into a path at all.
			return null;
		}
	}
}
