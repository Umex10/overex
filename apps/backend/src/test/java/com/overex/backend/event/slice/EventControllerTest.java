package com.overex.backend.event.slice;

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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overex.backend.auth.config.JacksonConfig;
import com.overex.backend.common.exception.ApiError;
import com.overex.backend.common.exception.OwnException;
import com.overex.backend.event.Event;
import com.overex.backend.event.EventController;
import com.overex.backend.event.EventService;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.utils.EventTestDataFactory;

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
 * {@link #event} is the shared fixture; JUnit creates a new test instance per
 * test, so every test gets a fresh copy.
 */
@WebMvcTest(EventController.class)
@Import(JacksonConfig.class)
public class EventControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @MockitoBean
        private EventService eventService;

        /** The default event every test works with (see the class comment). */
        private final Event event = EventTestDataFactory.createDefaultEvent();

        @Test
        void shouldGetEvents() throws Exception {
                List<Event> events = EventTestDataFactory.createEventList();
                when(eventService.getEvents()).thenReturn(events);

                mockMvc.perform(get("/event"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$", hasSize(events.size())))
                                .andExpect(jsonPath("$[*].title").value(containsInAnyOrder(
                                                events.stream().map(Event::getTitle).toArray())))
                                .andExpect(jsonPath("$[*].description").value(containsInAnyOrder(
                                                events.stream().map(Event::getDescription).toArray())))
                                .andExpect(jsonPath("$[*].venue").value(containsInAnyOrder(
                                                events.stream().map(Event::getVenue).toArray())))
                                .andExpect(jsonPath("$[*].city").value(containsInAnyOrder(
                                                events.stream().map(Event::getCity).toArray())))
                                .andExpect(jsonPath("$[*].startsAt").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$[*].saleStartsAt").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getSaleStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$[*].imageUrl").value(containsInAnyOrder(
                                                events.stream().map(Event::getImageUrl).toArray())))
                                .andExpect(jsonPath("$[*].status").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getStatus().name()).toArray())))
                                .andExpect(jsonPath("$[*].id").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getId().toString()).toArray())))
                                .andExpect(jsonPath("$[*].createdAt").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getCreatedAt().toString()).toArray())))
                                .andExpect(jsonPath("$[*].updatedAt").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getUpdatedAt().toString()).toArray())));
        }

        @Test
        void shouldGetEvent() throws Exception {
                when(eventService.getEvent(any())).thenReturn(event);

                mockMvc.perform(get("/event/{eventId}", event.getId()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(event.getId().toString()))
                                .andExpect(jsonPath("$.title").value(event.getTitle()))
                                .andExpect(jsonPath("$.description").value(event.getDescription()))
                                .andExpect(jsonPath("$.venue").value(event.getVenue()))
                                .andExpect(jsonPath("$.city").value(event.getCity()))
                                .andExpect(jsonPath("$.startsAt").value(event.getStartsAt().toString()))
                                .andExpect(jsonPath("$.saleStartsAt").value(event.getSaleStartsAt().toString()))
                                .andExpect(jsonPath("$.imageUrl").value(event.getImageUrl()))
                                .andExpect(jsonPath("$.status").value(event.getStatus().name()))
                                .andExpect(jsonPath("$.createdAt").value(event.getCreatedAt().toString()))
                                .andExpect(jsonPath("$.updatedAt").value(event.getUpdatedAt().toString()));

        }

        @Test
        void shouldNotGetMissingEvent() throws Exception {
                when(eventService.getEvent(any()))
                                .thenThrow(new EntityNotFoundException("The Event with the given id was not found"));

                mockMvc.perform(get("/event/{eventId}", event.getId()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                                .andExpect(jsonPath("$.path").value("/event/" + event.getId()));
        }

        /** An id that is no UUID is a client error (400), not the catch-all 500. */
        @Test
        void shouldRejectEventIdThatIsNoUuid() throws Exception {
                mockMvc.perform(get("/event/{eventId}", "abc"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors[0].field").value("eventId"));

                verify(eventService, never()).getEvent(any());
        }

        @Test
        void shouldCreateEvent() throws Exception {
                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();

                String jsonRequest = objectMapper.writeValueAsString(createEventDto);

                when(eventService.createEvent(createEventDto)).thenReturn(event);

                mockMvc.perform(post("/event")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").value(event.getId().toString()))
                                .andExpect(jsonPath("$.title").value(event.getTitle()))
                                .andExpect(jsonPath("$.description").value(event.getDescription()))
                                .andExpect(jsonPath("$.venue").value(event.getVenue()))
                                .andExpect(jsonPath("$.city").value(event.getCity()))
                                .andExpect(jsonPath("$.startsAt").value(event.getStartsAt().toString()))
                                .andExpect(jsonPath("$.saleStartsAt").value(event.getSaleStartsAt().toString()))
                                .andExpect(jsonPath("$.imageUrl").value(event.getImageUrl()))
                                .andExpect(jsonPath("$.status").value(event.getStatus().name()))
                                .andExpect(jsonPath("$.createdAt").value(event.getCreatedAt().toString()))
                                .andExpect(jsonPath("$.updatedAt").value(event.getUpdatedAt().toString()));
        }

        @Test
        void shouldRejectInvalidCreatePayload() throws Exception {
                CreateEventDto createEventDto = EventTestDataFactory.createInvalidCreateEventDto();

                String jsonRequest = objectMapper.writeValueAsString(createEventDto);

                mockMvc.perform(post("/event")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors[*].field").value(hasItems(
                                                "title", "description", "venue", "city", "startsAt", "saleStartsAt",
                                                "imageUrl")));

                // @Valid rejects the request before the controller method runs
                verify(eventService, never()).createEvent(any());
        }

        @Test
        void shouldDeleteEvent() throws Exception {
                doNothing().when(eventService).deleteEvent(any());

                mockMvc.perform(delete("/event/{eventId}", event.getId()))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));
        }

        @Test
        void shouldNotDeleteMissingEvent() throws Exception {

                doThrow(new EntityNotFoundException("The Event with the given id was not found")).when(eventService)
                                .deleteEvent(any());

                mockMvc.perform(delete("/event/{eventId}", event.getId()))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                                .andExpect(jsonPath("$.path").value("/event/" + event.getId()));
        }

        @Test
        void shouldUpdateEvent() throws Exception {
                UpdateEventDto updateEventDto = EventTestDataFactory.createUpdateEventDto();
                Event updatedEvent = EventTestDataFactory.createUpdatedEvent();

                String jsonRequest = objectMapper.writeValueAsString(updateEventDto);

                when(eventService.updateEvent(updateEventDto, event.getId())).thenReturn(updatedEvent);

                mockMvc.perform(patch("/event/{eventId}", event.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(updatedEvent.getId().toString()))
                                .andExpect(jsonPath("$.title").value(updatedEvent.getTitle()))
                                .andExpect(jsonPath("$.description").value(updatedEvent.getDescription()))
                                .andExpect(jsonPath("$.venue").value(updatedEvent.getVenue()))
                                .andExpect(jsonPath("$.city").value(updatedEvent.getCity()))
                                .andExpect(jsonPath("$.startsAt").value(updatedEvent.getStartsAt().toString()))
                                .andExpect(jsonPath("$.saleStartsAt").value(updatedEvent.getSaleStartsAt().toString()))
                                .andExpect(jsonPath("$.imageUrl").value(updatedEvent.getImageUrl()))
                                .andExpect(jsonPath("$.status").value(updatedEvent.getStatus().name()))
                                .andExpect(jsonPath("$.createdAt").value(updatedEvent.getCreatedAt().toString()))
                                .andExpect(jsonPath("$.updatedAt").value(updatedEvent.getUpdatedAt().toString()));
        }

        @Test
        void shouldRejectInvalidUpdatePayload() throws Exception {
                String jsonRequest = objectMapper
                                .writeValueAsString(EventTestDataFactory.createInvalidUpdateEventDto());

                mockMvc.perform(patch("/event/{eventId}", event.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.errors[*].field").value(hasItems(
                                                "title", "description", "venue", "city", "startsAt", "saleStartsAt",
                                                "imageUrl")));

                // @Valid rejects the request before the controller method runs
                verify(eventService, never()).updateEvent(any(), any());
        }

        @Test
        void shouldNotUpdateMissingEvent() throws Exception {
                String jsonRequest = objectMapper.writeValueAsString(EventTestDataFactory.createUpdateEventDto());

                when(eventService.updateEvent(any(), any()))
                                .thenThrow(new EntityNotFoundException("The Event with the given id was not found"));

                mockMvc.perform(patch("/event/{eventId}", event.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.status").value(404))
                                .andExpect(jsonPath("$.error").value("Not Found"))
                                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                                .andExpect(jsonPath("$.path").value("/event/" + event.getId()));
        }

        /**
         * The business rules themselves are covered by {@code EventServiceTest}; here
         * only the mapping of a rejected rule to a 400 with field errors is checked.
         */
        @Test
        void shouldNotUpdateEventRejectedByService() throws Exception {
                String jsonRequest = objectMapper
                                .writeValueAsString(EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart());

                when(eventService.updateEvent(any(), any()))
                                .thenThrow(new OwnException(List.of(
                                                new ApiError.FieldError("saleStartsAt",
                                                                "The sale must start before the event starts."))));

                mockMvc.perform(patch("/event/{eventId}", event.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("There is 1 error"))
                                .andExpect(jsonPath("$.errors[0].field").value("saleStartsAt"))
                                .andExpect(jsonPath("$.errors[0].message")
                                                .value("The sale must start before the event starts."));
        }

}
