package com.overex.backend.event.slice;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overex.backend.auth.security.CustomUserDetailsService;
import com.overex.backend.auth.security.config.SecurityConfig;
import com.overex.backend.auth.utils.AuthTestClient;
import com.overex.backend.auth.utils.AuthTestDataFactory;
import com.overex.backend.auth.utils.TestRsaKeys;
import com.overex.backend.common.config.JacksonConfig;
import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.Event;
import com.overex.backend.event.EventController;
import com.overex.backend.event.EventService;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.dto.responses.EventDetailsResponseDto;
import com.overex.backend.event.dto.responses.EventResponseDto;
import com.overex.backend.event.utils.EventTestDataFactory;
import com.overex.backend.user.User;
import com.overex.backend.user.UserRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Slice tests for {@link EventController}: only the web layer runs
 * ({@code @WebMvcTest}), {@link EventService} is a Mockito mock.
 *
 * <p>
 * Covers the mapping of every endpoint (status code and JSON body), bean
 * validation of the create and update payloads, and how exceptions of the
 * service end up as 404 or 400 responses. The business rules themselves are
 * tested in {@code EventServiceTest}.
 *
 * <p>
 * Every test builds its own data from {@link EventTestDataFactory}.
 */
@WebMvcTest(EventController.class)
@Import({ JacksonConfig.class, SecurityConfig.class, CustomUserDetailsService.class,
                TestRsaKeys.PublicKeyConfig.class })
public class EventControllerTest {


        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

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
        void shouldGetEventsWithDefaultPageableAndWithoutCategory() throws Exception {

                Pageable pageable = EventTestDataFactory.createDefaultPageable();
                Page<EventResponseDto> page = EventTestDataFactory.createEventResponsePage();
                List<EventResponseDto> events = page.getContent();
                when(eventService.getEvents(null, pageable)).thenReturn(page);

                // the mocked service returns the factory page as it is, so id and
                // timestamps are fixed values and compared exactly
                mockMvc.perform(get("/events"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(events.size())))
                                .andExpect(jsonPath("$.content[*].id").value(contains(
                                                events.stream().map(e -> e.id().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].title").value(contains(
                                                events.stream().map(EventResponseDto::title).toArray())))
                                .andExpect(jsonPath("$.content[*].description").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::description).toArray())))
                                .andExpect(jsonPath("$.content[*].venue").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::venue).toArray())))
                                .andExpect(jsonPath("$.content[*].city").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::city).toArray())))
                                .andExpect(jsonPath("$.content[*].startsAt").value(contains(
                                                events.stream().map(e -> e.startsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].saleStartsAt").value(containsInAnyOrder(
                                                events.stream().map(e -> e.saleStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].imageUrl").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::imageUrl).toArray())))
                                // no POST involved, so the factory events keep their own status
                                .andExpect(jsonPath("$.content[*].status").value(containsInAnyOrder(
                                                events.stream().map(e -> e.status().name()).toArray())))
                                .andExpect(jsonPath("$.content[*].createdAt").value(contains(
                                                events.stream().map(e -> e.createdAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].updatedAt").value(contains(
                                                events.stream().map(e -> e.updatedAt().toString()).toArray())))

                                .andExpect(jsonPath("$.page.size").value(EventTestDataFactory.DEFAULT_PAGE_SIZE))
                                .andExpect(jsonPath("$.page.number").value(EventTestDataFactory.DEFAULT_PAGE))
                                .andExpect(jsonPath("$.page.totalElements").value(events.size()))
                                .andExpect(jsonPath("$.page.totalPages").value(
                                                Math.ceilDiv(events.size(), EventTestDataFactory.DEFAULT_PAGE_SIZE)));
        }

        @Test
        void shouldGetEventsWithDefaultPageableAndWithCategory() throws Exception {

                Pageable pageable = EventTestDataFactory.createDefaultPageable();
                Page<EventResponseDto> page = EventTestDataFactory.createFestivalEventResponsePage();
                List<EventResponseDto> events = page.getContent();

                when(eventService.getEvents(EventTestDataFactory.DEFAULT_CATEGORY_ID, pageable)).thenReturn(page);

                // the mocked service returns the factory page as it is, so id and
                // timestamps are fixed values and compared exactly
                mockMvc.perform(get(String.format("/events?categoryId=%s", EventTestDataFactory.DEFAULT_CATEGORY_ID)))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(events.size())))
                                .andExpect(jsonPath("$.content[*].id").value(contains(
                                                events.stream().map(e -> e.id().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].category.id").value(containsInAnyOrder(
                                                events.stream().map(e -> e.category().id().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].title").value(contains(
                                                events.stream().map(EventResponseDto::title).toArray())))
                                .andExpect(jsonPath("$.content[*].description").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::description).toArray())))
                                .andExpect(jsonPath("$.content[*].venue").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::venue).toArray())))
                                .andExpect(jsonPath("$.content[*].city").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::city).toArray())))
                                .andExpect(jsonPath("$.content[*].startsAt").value(contains(
                                                events.stream().map(e -> e.startsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].saleStartsAt").value(containsInAnyOrder(
                                                events.stream().map(e -> e.saleStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].imageUrl").value(containsInAnyOrder(
                                                events.stream().map(EventResponseDto::imageUrl).toArray())))
                                // no POST involved, so the factory events keep their own status
                                .andExpect(jsonPath("$.content[*].status").value(containsInAnyOrder(
                                                events.stream().map(e -> e.status().name()).toArray())))
                                .andExpect(jsonPath("$.content[*].createdAt").value(contains(
                                                events.stream().map(e -> e.createdAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].updatedAt").value(contains(
                                                events.stream().map(e -> e.updatedAt().toString()).toArray())))

                                .andExpect(jsonPath("$.page.size").value(EventTestDataFactory.DEFAULT_PAGE_SIZE))
                                .andExpect(jsonPath("$.page.number").value(EventTestDataFactory.DEFAULT_PAGE))
                                .andExpect(jsonPath("$.page.totalElements").value(events.size()))
                                .andExpect(jsonPath("$.page.totalPages").value(
                                                Math.ceilDiv(events.size(), EventTestDataFactory.DEFAULT_PAGE_SIZE)));
        }

        @Test
        void shouldGetEvent() throws Exception {
                Event event = EventTestDataFactory.createDefaultEvent();

                EventDetailsResponseDto eventDetailsResponseDto = EventTestDataFactory.createEventDetailsResponseDto();
                EventResponseDto eventResponseDto = eventDetailsResponseDto.event();
                when(eventService.getEvent(any())).thenReturn(eventDetailsResponseDto);

                mockMvc.perform(get("/events/{eventId}", event.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.event.id").value(eventResponseDto.id().toString()))
                                .andExpect(jsonPath("$.event.title").value(eventResponseDto.title()))
                                .andExpect(jsonPath("$.event.description").value(eventResponseDto.description()))
                                .andExpect(jsonPath("$.event.venue").value(eventResponseDto.venue()))
                                .andExpect(jsonPath("$.event.city").value(eventResponseDto.city()))
                                .andExpect(jsonPath("$.event.startsAt").value(eventResponseDto.startsAt().toString()))
                                .andExpect(jsonPath("$.event.saleStartsAt").value(eventResponseDto.saleStartsAt().toString()))
                                .andExpect(jsonPath("$.event.imageUrl").value(eventResponseDto.imageUrl()))
                                .andExpect(jsonPath("$.event.status").value(eventResponseDto.status().name()))
                                .andExpect(jsonPath("$.event.createdAt").value(eventResponseDto.createdAt().toString()))
                                .andExpect(jsonPath("$.event.updatedAt").value(eventResponseDto.updatedAt().toString()))
                                // a DRAFT far ahead: draft and publish allowed, cancel blocked
                                .andExpect(jsonPath("$.statusOptions.DRAFT.disabled").value(false))
                                .andExpect(jsonPath("$.statusOptions.PUBLISHED.disabled").value(false))
                                .andExpect(jsonPath("$.statusOptions.CANCELED.disabled").value(true))
                                .andExpect(jsonPath("$.statusOptions.CANCELED.why")
                                                .value(EventTestDataFactory.DRAFT_CANCEL_BLOCKED_WHY));

        }

        @Test
        void shouldNotGetMissingEvent() throws Exception {
                Event event = EventTestDataFactory.createDefaultEvent();

                when(eventService.getEvent(any()))
                                .thenThrow(new EntityNotFoundException("The Event with the given id was not found"));

                mockMvc.perform(get("/events/{eventId}", event.getId()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                                .andExpect(jsonPath("$.path").value("/events/" + event.getId()));
        }

        /** An id that is no UUID is a client error (400), not the catch-all 500. */
        @Test
        void shouldRejectEventIdThatIsNoUuid() throws Exception {
                mockMvc.perform(get("/events/{eventId}", "abc"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors[0].field").value("eventId"));

                verify(eventService, never()).getEvent(any());
        }

        @Test
        void shouldCreateEvent() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();
                EventResponseDto event = EventTestDataFactory.createDefaultEventResponseDto();

                String jsonRequest = objectMapper.writeValueAsString(createEventDto);

                when(eventService.createEvent(user.getId(), createEventDto)).thenReturn(event);

                mockMvc.perform(post("/events")
                                .with(AuthTestClient.authenticatedAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(event.id().toString()))
                                .andExpect(jsonPath("$.title").value(event.title()))
                                .andExpect(jsonPath("$.description").value(event.description()))
                                .andExpect(jsonPath("$.venue").value(event.venue()))
                                .andExpect(jsonPath("$.city").value(event.city()))
                                .andExpect(jsonPath("$.startsAt").value(event.startsAt().toString()))
                                .andExpect(jsonPath("$.saleStartsAt").value(event.saleStartsAt().toString()))
                                .andExpect(jsonPath("$.imageUrl").value(event.imageUrl()))
                                .andExpect(jsonPath("$.status").value(event.status().name()))
                                .andExpect(jsonPath("$.createdAt").value(event.createdAt().toString()))
                                .andExpect(jsonPath("$.updatedAt").value(event.updatedAt().toString()));
        }

        @Test
        void shouldRejectInvalidCreatePayload() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                CreateEventDto createEventDto = EventTestDataFactory.createInvalidCreateEventDto();

                String jsonRequest = objectMapper.writeValueAsString(createEventDto);

                mockMvc.perform(post("/events")
                                .with(AuthTestClient.authenticatedAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors[*].field").value(hasItems(
                                                "title", "description", "categoryId", "venue", "city", "startsAt",
                                                "saleStartsAt", "imageUrl")));

                // @Valid rejects the request before the controller method runs
                verify(eventService, never()).createEvent(any(), any());
        }

        @Test
        void shouldDeleteEvent() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                doNothing().when(eventService).deleteEvent(any());

                mockMvc.perform(delete("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user)))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));
        }

        @Test
        void shouldNotDeleteMissingEvent() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                doThrow(new EntityNotFoundException("The Event with the given id was not found")).when(eventService)
                                .deleteEvent(any());

                mockMvc.perform(delete("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user)))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                                .andExpect(jsonPath("$.path").value("/events/" + event.getId()));
        }

        @Test
        void shouldUpdateEvent() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                UpdateEventDto updateEventDto = EventTestDataFactory.createUpdateEventDto();
                EventResponseDto updatedEvent = EventTestDataFactory.createUpdatedEventResponseDto();

                String jsonRequest = objectMapper.writeValueAsString(updateEventDto);

                when(eventService.updateEvent(updateEventDto, event.getId())).thenReturn(updatedEvent);

                mockMvc.perform(patch("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(updatedEvent.id().toString()))
                                .andExpect(jsonPath("$.title").value(updatedEvent.title()))
                                .andExpect(jsonPath("$.description").value(updatedEvent.description()))
                                .andExpect(jsonPath("$.venue").value(updatedEvent.venue()))
                                .andExpect(jsonPath("$.city").value(updatedEvent.city()))
                                .andExpect(jsonPath("$.startsAt").value(updatedEvent.startsAt().toString()))
                                .andExpect(jsonPath("$.saleStartsAt").value(updatedEvent.saleStartsAt().toString()))
                                .andExpect(jsonPath("$.imageUrl").value(updatedEvent.imageUrl()))
                                .andExpect(jsonPath("$.status").value(updatedEvent.status().name()))
                                .andExpect(jsonPath("$.createdAt").value(updatedEvent.createdAt().toString()))
                                .andExpect(jsonPath("$.updatedAt").value(updatedEvent.updatedAt().toString()));
        }

        @Test
        void shouldRejectInvalidUpdatePayload() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                String jsonRequest = objectMapper
                                .writeValueAsString(EventTestDataFactory.createInvalidUpdateEventDto());

                mockMvc.perform(patch("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors[*].field").value(hasItems(
                                                "title", "description", "categoryId", "venue", "city", "startsAt",
                                                "saleStartsAt", "imageUrl")));

                // @Valid rejects the request before the controller method runs
                verify(eventService, never()).updateEvent(any(), any());
        }

        @Test
        void shouldNotUpdateMissingEvent() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                String jsonRequest = objectMapper.writeValueAsString(EventTestDataFactory.createUpdateEventDto());

                when(eventService.updateEvent(any(), any()))
                                .thenThrow(new EntityNotFoundException("The Event with the given id was not found"));

                mockMvc.perform(patch("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                                .andExpect(jsonPath("$.path").value("/events/" + event.getId()));
        }

        /**
         * The business rules themselves are covered by {@code EventServiceTest}; here
         * only the mapping of a rejected rule to a 400 with field errors is checked.
         */
        @Test
        void shouldNotUpdateEventRejectedByService() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                String jsonRequest = objectMapper
                                .writeValueAsString(EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart());

                when(eventService.updateEvent(any(), any()))
                                .thenThrow(new OwnException(List.of(
                                                new ApiError.FieldError("saleStartsAt",
                                                                "The sale must start before the event starts."))));

                mockMvc.perform(patch("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("There is 1 error"))
                                .andExpect(jsonPath("$.errors[0].field").value("saleStartsAt"))
                                .andExpect(jsonPath("$.errors[0].message")
                                                .value("The sale must start before the event starts."));
        }

        /**
         * Someone else saved the event after the client loaded it: the service throws
         * {@link ObjectOptimisticLockingFailureException}, the handler answers 409.
         */
        @Test
        void shouldNotUpdateEventChangedInTheMeantime() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                String jsonRequest = objectMapper.writeValueAsString(EventTestDataFactory.createUpdateEventDto());

                when(eventService.updateEvent(any(), any()))
                                .thenThrow(new ObjectOptimisticLockingFailureException(Event.class, event.getId()));

                mockMvc.perform(patch("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isConflict())
                                .andExpect(jsonPath("$.status").value(409))
                                .andExpect(jsonPath("$.message")
                                                .value("This resource was changed in the meantime. Reload it and try again."))
                                .andExpect(jsonPath("$.path").value("/events/" + event.getId()))
                                .andExpect(jsonPath("$.errors", hasSize(0)));
        }

        /**
         * The service refuses the call (e.g. a {@code @PreAuthorize} rule): the
         * handler answers 403 with a fixed text, never the exception's own message.
         */
        @Test
        void shouldNotDeleteEventWhenAccessIsDenied() throws Exception {
                User user = AuthTestDataFactory.createDefaultUser();
                Event event = EventTestDataFactory.createDefaultEvent();

                doThrow(new AccessDeniedException("internal reason")).when(eventService).deleteEvent(any());

                mockMvc.perform(delete("/events/{eventId}", event.getId())
                                .with(AuthTestClient.authenticatedAs(user)))
                                .andExpect(status().isForbidden())
                                .andExpect(jsonPath("$.status").value(403))
                                .andExpect(jsonPath("$.message")
                                                .value("You do not have permission to access this resource."))
                                .andExpect(jsonPath("$.path").value("/events/" + event.getId()))
                                .andExpect(jsonPath("$.errors", hasSize(0)));
        }

}
