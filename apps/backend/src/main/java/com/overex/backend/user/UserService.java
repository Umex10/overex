package com.overex.backend.user;

import java.util.UUID;

import org.springframework.stereotype.Service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Business logic around user accounts. The controller only reads the user id
 * from the token; loading the user and the "not found" case live here.
 */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository userRepository;

	/**
	 * Loads the signed-in user.
	 *
	 * @param userId the id from the access token's {@code sub} claim
	 * @return the stored user
	 * @throws EntityNotFoundException if the account was deleted after the token
	 *                                 was issued (answered as 404)
	 */
	public User getCurrentUser(UUID userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new EntityNotFoundException("User " + userId + " no longer exists."));
	}

	/**
	 * Checks that a user exists, without loading it: the database only answers
	 * yes or no.
	 *
	 * @param userId the id from the access token's {@code sub} claim
	 * @throws EntityNotFoundException if the account was deleted after the token
	 *                                 was issued (answered as 404)
	 */
	public void ensureUserExists(UUID userId) {
		if (!userRepository.existsById(userId)) {
			throw new EntityNotFoundException("User " + userId + " no longer exists.");
		}
	}

	/**
	 * A reference to a user, without loading it: no database query, only the id
	 * is set. Meant for setting a relation, e.g. the creator of an event.
	 *
	 * <p>Does not check that the user exists. If it doesn't, saving the entity
	 * that points to it fails on the foreign key.
	 *
	 * @param userId the id from the access token's {@code sub} claim
	 * @return a lazy proxy of the user with only its id
	 */
	public User getReference(UUID userId) {
		return userRepository.getReferenceById(userId);
	}
}
