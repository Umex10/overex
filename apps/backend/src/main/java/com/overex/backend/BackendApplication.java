package com.overex.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.overex.backend.auth.config.DevKeyGenerator;

/**
 * Entry point of the Overex core monolith.
 *
 * <p>Overex is a ticketing platform for high-demand events. This backend
 * handles sign-up, sign-in, refresh-token rotation, role-based access and the
 * Swagger UI (taken over from AuthKit); events and orders are added here next.
 */
@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		SpringApplication app = new SpringApplication(BackendApplication.class);
		// Creates the JWT keys on the first start after a clone (see DevKeyGenerator).
		app.addListeners(new DevKeyGenerator());
		app.run(args);
	}

}
