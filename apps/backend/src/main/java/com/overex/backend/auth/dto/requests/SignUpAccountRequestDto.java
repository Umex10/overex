package com.overex.backend.auth.dto.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request payload for the {@code POST /auth/sign-up} endpoint.
 *
 * <p>Carries the attributes required to create a new {@code User}; each field is
 * validated server-side.
 *
 * <p>There is deliberately no {@code role} field: every new account is a
 * {@code USER}. A {@code "role"} sent by a client is ignored (unknown JSON
 * properties are skipped), so nobody can register themselves as {@code ADMIN}.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SignUpAccountRequestDto {

	@NotBlank(message = "Name is required")
	@Size(min = 2, message = "Name must be at least {min} characters.")
	private String name;

	@NotBlank(message = "Email is required")
	@Email(message = "Please enter a valid email address.")
	private String email;

	@NotBlank(message = "Phone is required")
	@Size(min = 10, message = "Phone must be at least {min} characters.")
	private String phone;

	@NotBlank(message = "Password is required")
	@Size(min = 8, message = "Password must be at least {min} characters.")
	private String password;
}
