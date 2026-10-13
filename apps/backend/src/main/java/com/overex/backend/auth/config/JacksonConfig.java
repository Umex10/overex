package com.overex.backend.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Jackson configuration shared by the entire application.
 *
 * <p>Registers the {@link JavaTimeModule} so the {@code java.time} types used by
 * the DTOs are serialised as ISO-8601 strings instead of numeric timestamps.
 */
@Configuration
public class JacksonConfig {

	/**
	 * Builds the application's primary {@link ObjectMapper}.
	 *
	 * @return an object mapper with the Java time module registered and
	 *         timestamp-style date serialisation disabled
	 */
	@Bean
	public ObjectMapper objectMapper() {
		return new ObjectMapper()
				.registerModule(new JavaTimeModule())
				.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
	}
}
