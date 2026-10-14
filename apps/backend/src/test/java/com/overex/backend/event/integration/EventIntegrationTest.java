package com.overex.backend.event.integration;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overex.backend.auth.utils.TestKeys;
import com.overex.backend.auth.utils.TkFactory;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.event.Event;
import com.overex.backend.event.EventMapper;
import com.overex.backend.event.EventRepository;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.utils.EventTestDataFactory;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class EventIntegrationTest {

    /**
     * Temporary folder for the generated key files. JUnit creates it before the
     * Spring context starts and deletes it after all tests of this class. It is
     * static because {@link #keyLocations} below is static and runs before any
     * test instance exists.
     */
    @TempDir
    static Path keyDir;

    /**
     * Runs while Spring builds the context, before the beans are created: writes
     * {@code private.pem} and {@code public.pem} into {@link #keyDir} and sets the
     * two key-location properties to these files. Values registered here win over
     * {@code application.properties} and {@code application-test.properties}.
     */
    @DynamicPropertySource
    static void keyLocations(DynamicPropertyRegistry registry) {
        TestKeys.writePemFiles(keyDir);
        registry.add("jwt.private-key-location", () -> keyDir.resolve("private.pem").toUri().toString());
        registry.add("jwt.public-key-location", () -> keyDir.resolve("public.pem").toUri().toString());
    }

    /** Sends requests through the whole application, like a client would. */
    @Autowired
    private MockMvc mockMvc;

    /** Turns the request DTOs into JSON. */
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EventMapper eventMapper;

    @BeforeEach
    void emptyDatabase() {
        eventRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void shouldGetEvents() throws Exception {
        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        List<Event> events = EventTestDataFactory.createEventList();

        for (Event event : events) {
            createEvent(accessTk, eventMapper.toCreateEventDto(event));
        }

        // findAll() has no fixed order, so every field is compared as a set
        // (containsInAnyOrder) instead of by index.
        mockMvc.perform(get("/event").header("Authorization", "Bearer " + accessTk))
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
                // created through POST, so every event starts as a draft
                .andExpect(jsonPath("$[*].status").value(everyItem(is(Status.DRAFT.name()))))
                // id and timestamps are generated by the database
                .andExpect(jsonPath("$[*].id").value(everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].createdAt").value(everyItem(notNullValue())))
                .andExpect(jsonPath("$[*].updatedAt").value(everyItem(notNullValue())));
    }

    @Test
    void shouldGetEvent() throws Exception {
        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();

        Event event = createEvent(accessTk, createEventDto);

        mockMvc.perform(get(String.format("/event/%s", event.getId().toString())).header("Authorization",
                "Bearer " + accessTk))
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
    void shouldCreateEvent() throws Exception {
        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();

        Event event = createEvent(accessTk, createEventDto);

        mockMvc.perform(get(String.format("/event/%s", event.getId().toString())).header("Authorization",
                "Bearer " + accessTk))
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
    void shouldDeleteEvent() throws Exception {

        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();

        Event event = createEvent(accessTk, createEventDto);

        mockMvc.perform(
                delete(String.format("/event/%s", event.getId().toString())).header("Authorization",
                        "Bearer " + accessTk))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

    }

    @Test
    void shouldUpdateEvent() throws Exception {
        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();

        Event event = createEvent(accessTk, createEventDto);

        Event updatedEvent = EventTestDataFactory.createUpdatedEvent();
        UpdateEventDto updatedEventDto = EventTestDataFactory.createUpdateEventDto();

        String jsonRequest = objectMapper.writeValueAsString(updatedEventDto);

        mockMvc.perform(patch(String.format("/event/%s", event.getId().toString())).header("Authorization",
                "Bearer " + accessTk)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId().toString()))
                .andExpect(jsonPath("$.title").value(updatedEvent.getTitle()))
                .andExpect(jsonPath("$.description").value(updatedEvent.getDescription()))
                .andExpect(jsonPath("$.venue").value(updatedEvent.getVenue()))
                .andExpect(jsonPath("$.city").value(updatedEvent.getCity()))
                .andExpect(jsonPath("$.startsAt").value(updatedEvent.getStartsAt().toString()))
                .andExpect(jsonPath("$.saleStartsAt").value(updatedEvent.getSaleStartsAt().toString()))
                .andExpect(jsonPath("$.imageUrl").value(updatedEvent.getImageUrl()))
                .andExpect(jsonPath("$.status").value(updatedEvent.getStatus().name()))
                .andExpect(jsonPath("$.createdAt").value(event.getCreatedAt().toString()))
                .andExpect(jsonPath("$.updatedAt").value(not(event.getUpdatedAt().toString())));
    }

    @Test
    void shouldNotUpdateEventWithAlreadyStartedEvent() throws Exception {
        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        CreateEventDto createEventDto = EventTestDataFactory.createCreatePastEventDto();

        String jsonRequest = objectMapper.writeValueAsString(createEventDto);

        // @Future on field startsAt in CreateEventDto will trigger

        mockMvc.perform(post("/event")
                .header("Authorization", "Bearer " + accessTk)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldNotUpdateEventWithPastStartsAt() throws Exception {
        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();

        Event event = createEvent(accessTk, createEventDto);

        UpdateEventDto updatedEventDto = EventTestDataFactory.createUpdateEventDtoWithPastStartsAt();

        String jsonRequest = objectMapper.writeValueAsString(updatedEventDto);

        mockMvc.perform(patch(String.format("/event/%s", event.getId().toString())).header("Authorization",
                "Bearer " + accessTk)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("There is 1 error"))
                .andExpect(jsonPath("$.errors[0].field").value("startsAt"))
                .andExpect(jsonPath("$.errors[0].message").value("The new start time must be in the future."));
    }

    @Test
    void shouldNotUpdateEventWithSaleAtAfterStartAt() throws Exception {
        String accessTk = TkFactory.signUpAndReturnAccessTk(mockMvc, objectMapper);

        CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto();

        Event event = createEvent(accessTk, createEventDto);

        UpdateEventDto updatedEventDto = EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart();

        String jsonRequest = objectMapper.writeValueAsString(updatedEventDto);

        mockMvc.perform(patch(String.format("/event/%s", event.getId().toString())).header("Authorization",
                "Bearer " + accessTk)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("There is 1 error"))
                .andExpect(jsonPath("$.errors[0].field").value("saleStartsAt"))
                .andExpect(jsonPath("$.errors[0].message").value("The sale must start before the event starts."));
    }

    private Event createEvent(String accessTk, CreateEventDto createEventDto) throws Exception {

        String jsonRequest = objectMapper.writeValueAsString(createEventDto);

        MvcResult result = mockMvc.perform(post("/event")
                .header("Authorization", "Bearer " + accessTk)
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isCreated())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return objectMapper.readValue(body, Event.class);

    }

}
