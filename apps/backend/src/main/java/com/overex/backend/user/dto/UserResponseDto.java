package com.overex.backend.user.dto;

import com.overex.backend.common.entities.Role;
import com.overex.backend.common.entities.User;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Safe projection of a {@link User} returned to authenticated clients.
 *
 * <p>Deliberately omits the password hash. This is what {@code GET /me} returns
 * so the frontend can greet the user and render role-aware UI.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserResponseDto {

	private String name;
	private String email;
	private String phone;
	private Role role;

	/**
	 * Maps a persisted {@link User} onto the password-free response projection.
	 *
	 * @return the populated DTO
	 */
	public static UserResponseDto from(User user) {
		return UserResponseDto.builder()
				.name(user.getName())
				.email(user.getEmail())
				.phone(user.getPhone())
				.role(user.getRole())
				.build();
	}
}
