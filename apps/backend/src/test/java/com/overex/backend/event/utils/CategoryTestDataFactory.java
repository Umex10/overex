package com.overex.backend.event.utils;

import java.util.List;
import java.util.UUID;

import com.overex.backend.event.Category;

/**
 * Reusable category fixtures.
 *
 * <p>
 * Every value is defined once as a constant below and only referenced by the
 * factory methods, so tests can assert against the same ids and titles.
 */
public final class CategoryTestDataFactory {

	private CategoryTestDataFactory() {
	}

	// The default category, most events refer to it.
	public static final UUID DEFAULT_CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
	public static final String DEFAULT_CATEGORY_TITLE = "Festival";

	// A second category, so lists can be filtered.
	public static final UUID CONCERT_CATEGORY_ID = UUID.fromString("66666666-6666-6666-6666-666666666666");
	public static final String CONCERT_CATEGORY_TITLE = "Konzert";

	// A third category without events.
	public static final UUID THEATER_CATEGORY_ID = UUID.fromString("77777777-7777-7777-7777-777777777777");
	public static final String THEATER_CATEGORY_TITLE = "Theater";

	/** A persisted-looking category (with id). */
	public static Category createDefaultCategory() {
		return createCategory(DEFAULT_CATEGORY_ID, DEFAULT_CATEGORY_TITLE);
	}

	/** The concert category. */
	public static Category createConcertCategory() {
		return createCategory(CONCERT_CATEGORY_ID, CONCERT_CATEGORY_TITLE);
	}

	/** The theater category. */
	public static Category createTheaterCategory() {
		return createCategory(THEATER_CATEGORY_ID, THEATER_CATEGORY_TITLE);
	}

	/**
	 * All three categories: festival, concert and theater. Each call returns new
	 * instances, so tests can change them freely.
	 */
	public static List<Category> createCategoryList() {
		return List.of(createDefaultCategory(), createConcertCategory(), createTheaterCategory());
	}

	/** A new category without id, ready to be saved by a repository. */
	public static Category createUnsavedCategory(String title) {
		return Category.builder()
				.title(title)
				.build();
	}

	/**
	 * The titles of {@link #createCategoryList()} without ids, ready to be saved
	 * by a repository.
	 */
	public static List<Category> createUnsavedCategoryList() {
		return List.of(
				createUnsavedCategory(DEFAULT_CATEGORY_TITLE),
				createUnsavedCategory(CONCERT_CATEGORY_TITLE),
				createUnsavedCategory(THEATER_CATEGORY_TITLE));
	}

	private static Category createCategory(UUID id, String title) {
		return Category.builder()
				.id(id)
				.title(title)
				.build();
	}
}
