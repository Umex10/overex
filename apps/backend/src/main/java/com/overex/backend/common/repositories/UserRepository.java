package com.overex.backend.common.repositories;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.overex.backend.common.entities.User;

/**
 * Repository for {@link User} persistence operations.
 *
 * <p>Backs both the sign-up uniqueness checks (email and phone) and the Spring
 * Security user lookup performed by
 * {@link com.overex.backend.auth.security.CustomUserDetailsService}.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

	/**
	 * Finds a user by email address.
	 *
	 * @return the matching user, or {@link Optional#empty()} when none exists
	 */
	Optional<User> findByEmail(String email);

	/**
	 * Finds a user by phone number.
	 *
	 * @return the matching user, or {@link Optional#empty()} when none exists
	 */
	Optional<User> findByPhone(String phone);

}
