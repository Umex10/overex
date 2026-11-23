package com.overex.backend.event.slice;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.overex.backend.auth.security.CustomUserDetailsService;
import com.overex.backend.auth.security.config.SecurityConfig;
import com.overex.backend.auth.utils.AuthTestClient;
import com.overex.backend.auth.utils.AuthTestDataFactory;
import com.overex.backend.auth.utils.TestRsaKeys;
import com.overex.backend.common.config.JacksonConfig;
import com.overex.backend.event.EventService;
import com.overex.backend.event.MyEventsController;
import com.overex.backend.event.MyEventsFilter;
import com.overex.backend.event.dto.responses.CountsOfEventsDto;
import com.overex.backend.event.dto.responses.EventResponseDto;
import com.overex.backend.event.utils.EventTestDataFactory;
import com.overex.backend.event.utils.EventTestDataFactory.MyEventsFilterCase;
import com.overex.backend.user.User;
import com.overex.backend.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Slice tests for {@link MyEventsController}: the web layer with the real
 * {@link SecurityConfig} filter chain, {@link EventService} is a Mockito mock.
 *
 * <p>
 * The routes read the user id from the token, so every request is sent with
 * {@code authenticatedAs(user)} (an already verified JWT, see
 * {@code UserControllerTest}). Which events a tab holds is tested in
 * {@code EventServiceTest}; here only the mapping of the request to the
 * service call and of the result to JSON is checked.
 */
@WebMvcTest(MyEventsController.class)
@Import({ SecurityConfig.class, CustomUserDetailsService.class, TestRsaKeys.PublicKeyConfig.class,
		JacksonConfig.class })
public class MyEventsControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private EventService eventService;

	/**
	 * Only needed so the context starts: {@link CustomUserDetailsService} of the
	 * real security chain asks for a UserRepository in its constructor, but
	 * {@code @WebMvcTest} loads no repositories (no JPA, no database). Never
	 * called in these tests.
	 */
	@MockitoBean
	private UserRepository userRepository;

	@Test
	void shouldGetMyEventsWithDefaultPageableAndAllTab() throws Exception {
		// Arrange
		User user = AuthTestDataFactory.createDefaultUser();
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		Page<EventResponseDto> page = EventTestDataFactory.createMyEventResponsePage();
		List<EventResponseDto> events = page.getContent();

		// no ?filter in the URL → the controller's default ALL
		when(eventService.getMyEvents(user.getId(), null, MyEventsFilter.ALL, pageable)).thenReturn(page);

		// Act + Assert
		mockMvc.perform(get("/me/events").with(AuthTestClient.authenticatedAs(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(events.size())))
				.andExpect(jsonPath("$.content[*].id").value(contains(
						events.stream().map(e -> e.id().toString()).toArray())))
				.andExpect(jsonPath("$.content[*].title").value(contains(
						events.stream().map(EventResponseDto::title).toArray())))
				.andExpect(jsonPath("$.content[*].status").value(contains(
						events.stream().map(e -> e.status().name()).toArray())))
				.andExpect(jsonPath("$.content[*].startsAt").value(contains(
						events.stream().map(e -> e.startsAt().toString()).toArray())))

				.andExpect(jsonPath("$.page.size").value(EventTestDataFactory.DEFAULT_PAGE_SIZE))
				.andExpect(jsonPath("$.page.number").value(EventTestDataFactory.DEFAULT_PAGE))
				.andExpect(jsonPath("$.page.totalElements").value(events.size()))
				.andExpect(jsonPath("$.page.totalPages").value(
						Math.ceilDiv(events.size(), EventTestDataFactory.DEFAULT_PAGE_SIZE)));
	}

	/** ?filter=… reaches the service as the matching {@link MyEventsFilter}. */
	@Test
	void shouldGetMyEventsForEveryTab() throws Exception {
		// Arrange (shared by every tab)
		User user = AuthTestDataFactory.createDefaultUser();
		Pageable pageable = EventTestDataFactory.createDefaultPageable();

		for (MyEventsFilterCase tab : EventTestDataFactory.createMyEventsFilterCases()) {
			// Arrange
			Page<EventResponseDto> page = EventTestDataFactory.createMyEventResponsePage(tab.filter(), pageable);
			List<EventResponseDto> events = page.getContent();

			when(eventService.getMyEvents(user.getId(), null, tab.filter(), pageable)).thenReturn(page);

			// Act + Assert
			mockMvc.perform(get("/me/events")
					.param("filter", tab.filter().name())
					.with(AuthTestClient.authenticatedAs(user)))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content[*].id").value(contains(
							events.stream().map(e -> e.id().toString()).toArray())))
					.andExpect(jsonPath("$.page.totalElements").value(tab.expectedCount()));
		}
	}

	@Test
	void shouldGetMyEventsWithCategory() throws Exception {
		// Arrange
		User user = AuthTestDataFactory.createDefaultUser();
		Pageable pageable = EventTestDataFactory.createDefaultPageable();
		Page<EventResponseDto> page = EventTestDataFactory.createMyEventResponsePage();

		when(eventService.getMyEvents(user.getId(), EventTestDataFactory.DEFAULT_CATEGORY_ID, MyEventsFilter.ALL,
				pageable)).thenReturn(page);

		// Act + Assert
		mockMvc.perform(get("/me/events")
				.param("categoryId", EventTestDataFactory.DEFAULT_CATEGORY_ID.toString())
				.with(AuthTestClient.authenticatedAs(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content", hasSize(page.getContent().size())));
	}

	/** A tab that does not exist is a client error (400), not the catch-all 500. */
	@Test
	void shouldRejectUnknownFilter() throws Exception {
		// Arrange
		User user = AuthTestDataFactory.createDefaultUser();

		// Act + Assert
		mockMvc.perform(get("/me/events")
				.param("filter", "SOLD_OUT")
				.with(AuthTestClient.authenticatedAs(user)))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.status").value(400))
				.andExpect(jsonPath("$.errors[0].field").value("filter"))
				.andExpect(jsonPath("$.errors[0].message").value("The value 'SOLD_OUT' has the wrong format."));

		verifyNoInteractions(eventService);
	}

	/**
	 * A valid token whose user was deleted meanwhile: {@code ensureUserExists}
	 * in the service throws {@link EntityNotFoundException}, the handler answers
	 * 404.
	 */
	@Test
	void shouldNotGetMyEventsWhenUserNoLongerExists() throws Exception {
		// Arrange
		User user = AuthTestDataFactory.createDefaultUser();
		Pageable pageable = EventTestDataFactory.createDefaultPageable();

		when(eventService.getMyEvents(user.getId(), null, MyEventsFilter.ALL, pageable))
				.thenThrow(new EntityNotFoundException("User " + user.getId() + " no longer exists."));

		// Act + Assert
		mockMvc.perform(get("/me/events").with(AuthTestClient.authenticatedAs(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("User " + user.getId() + " no longer exists."))
				.andExpect(jsonPath("$.path").value("/me/events"));
	}

	/** Without a token the request is rejected before the controller runs. */
	@Test
	void shouldRejectAnonymousAccessToMyEvents() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/me/events"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(eventService);
	}

	@Test
	void shouldGetCountsOfEvents() throws Exception {
		// Arrange
		User user = AuthTestDataFactory.createDefaultUser();
		CountsOfEventsDto counts = EventTestDataFactory.createCountsOfEventsDto();

		when(eventService.getCountsOfEvents(user.getId())).thenReturn(counts);

		// Act + Assert
		mockMvc.perform(get("/me/events/counts").with(AuthTestClient.authenticatedAs(user)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.countOfAllEvents").value(counts.countOfAllEvents()))
				.andExpect(jsonPath("$.countOfDraftEvents").value(counts.countOfDraftEvents()))
				.andExpect(jsonPath("$.countOfPublishedEvents").value(counts.countOfPublishedEvents()))
				.andExpect(jsonPath("$.countOfCanceledEvents").value(counts.countOfCanceledEvents()))
				.andExpect(jsonPath("$.countOfFinishedEvents").value(counts.countOfFinishedEvents()));
	}

	/** Same as for the list: a deleted user gets 404, not counts of zero. */
	@Test
	void shouldNotGetCountsWhenUserNoLongerExists() throws Exception {
		// Arrange
		User user = AuthTestDataFactory.createDefaultUser();

		when(eventService.getCountsOfEvents(user.getId()))
				.thenThrow(new EntityNotFoundException("User " + user.getId() + " no longer exists."));

		// Act + Assert
		mockMvc.perform(get("/me/events/counts").with(AuthTestClient.authenticatedAs(user)))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.status").value(404))
				.andExpect(jsonPath("$.message").value("User " + user.getId() + " no longer exists."))
				.andExpect(jsonPath("$.path").value("/me/events/counts"));
	}

	@Test
	void shouldRejectAnonymousAccessToCounts() throws Exception {
		// Act + Assert
		mockMvc.perform(get("/me/events/counts"))
				.andExpect(status().isUnauthorized());

		verifyNoInteractions(eventService);
	}
}
