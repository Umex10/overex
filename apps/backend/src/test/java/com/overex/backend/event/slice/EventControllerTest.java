package com.overex.backend.event.slice;

import static org.hamcrest.Matchers.hasItems;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;


@WebMvcTest(EventController.class)
@Import(JacksonConfig.class)
public class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    public final UUID DEFAULT_EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final Event event = EventTestDataFactory.createDefaultEvent();


    @Test
    void shouldGetEvent() throws Exception {
        when(eventService.getEvent(any())).thenReturn(event);

        mockMvc.perform(get(String.format("/event/%s", DEFAULT_EVENT_ID)))
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

        mockMvc.perform(get(String.format("/event/%s", DEFAULT_EVENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                .andExpect(jsonPath("$.path").value(String.format("/event/%s", DEFAULT_EVENT_ID)));
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
    void shouldDeleteEvent() throws Exception {
        doNothing().when(eventService).deleteEvent(any());

        mockMvc.perform(delete(String.format("/event/%s", DEFAULT_EVENT_ID)))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void shouldNotDeleteMissingEvent() throws Exception {

        doThrow(new EntityNotFoundException("The Event with the given id was not found")).when(eventService)
                .deleteEvent(any());

        mockMvc.perform(delete(String.format("/event/%s", DEFAULT_EVENT_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                .andExpect(jsonPath("$.path").value(String.format("/event/%s", DEFAULT_EVENT_ID)));
    }

    @Test
    void shouldUpdateEvent() throws Exception {
        UpdateEventDto updateEventDto = EventTestDataFactory.createUpdateEventDto();
        Event updatedEvent = EventTestDataFactory.createUpdatedEvent();

        String jsonRequest = objectMapper.writeValueAsString(updateEventDto);

        when(eventService.updateEvent(updateEventDto, DEFAULT_EVENT_ID)).thenReturn(updatedEvent);

        mockMvc.perform(patch(String.format("/event/%s", DEFAULT_EVENT_ID))
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
        String jsonRequest = objectMapper.writeValueAsString(EventTestDataFactory.createInvalidUpdateEventDto());

        mockMvc.perform(patch(String.format("/event/%s", DEFAULT_EVENT_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.errors[*].field").value(hasItems(
                        "title", "description", "venue", "city", "startsAt", "saleStartsAt", "imageUrl")));

        // @Valid rejects the request before the controller method runs
        verify(eventService, never()).updateEvent(any(), any());
    }

    @Test
    void shouldNotUpdateMissingEvent() throws Exception {
        String jsonRequest = objectMapper.writeValueAsString(EventTestDataFactory.createUpdateEventDto());

        when(eventService.updateEvent(any(), any()))
                .thenThrow(new EntityNotFoundException("The Event with the given id was not found"));

        mockMvc.perform(patch(String.format("/event/%s", DEFAULT_EVENT_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("The Event with the given id was not found"))
                .andExpect(jsonPath("$.path").value(String.format("/event/%s", DEFAULT_EVENT_ID)));
    }

    /**
     * The business rules themselves are covered by {@code EventServiceTest}; here
     * only the mapping of a rejected rule to a 400 with field errors is checked.
     */
    @Test
    void shouldNotUpdateEventRejectedByService() throws Exception {
        String jsonRequest = objectMapper.writeValueAsString(EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart());

        when(eventService.updateEvent(any(), any()))
                .thenThrow(new OwnException(List.of(
                        new ApiError.FieldError("saleStartsAt", "The sale must start before the event starts."))));

        mockMvc.perform(patch(String.format("/event/%s", DEFAULT_EVENT_ID))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("There is 1 error"))
                .andExpect(jsonPath("$.errors[0].field").value("saleStartsAt"))
                .andExpect(jsonPath("$.errors[0].message").value("The sale must start before the event starts."));
    }

}
