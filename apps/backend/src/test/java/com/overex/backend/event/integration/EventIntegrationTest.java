package com.overex.backend.event.integration;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.in;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.overex.backend.auth.utils.AuthTestClient;
import com.overex.backend.auth.utils.TestRsaKeys;
import com.overex.backend.common.repositories.UserRepository;
import com.overex.backend.event.CategoryRepository;
import com.overex.backend.event.Event;
import com.overex.backend.event.EventRepository;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.requests.CreateEventDto;
import com.overex.backend.event.dto.requests.UpdateEventDto;
import com.overex.backend.event.utils.EventTestDataFactory;

/**
 * Integration tests for the event endpoints: the whole application runs
 * (security filter chain, controller, service, JPA) against the in-memory H2
 * database of the "test" profile. Nothing is mocked.
 *
 * <p>
 * Every test signs up a real user through {@link AuthTestClient} and sends its
 * access token, like a client would. Events are created through
 * {@code POST /event}, so id, status and timestamps come from the application;
 * that is why each test works with the event returned by
 * {@link #createEvent(String, CreateEventDto)} instead of a class-level
 * fixture.
 *
 * <p>
 * The business rules of the update are covered in detail by
 * {@code EventServiceTest}; here one rule each checks that a rejection reaches
 * the client as a 400 with field errors.
 *
 * <p>
 * Every event needs a category, so {@link #setUpDatabase()} stores two before
 * each test (festival and concert); the payloads refer to them through
 * {@link #categoryDefaultId} and {@link #categoryConcertId}.
 */
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
                TestRsaKeys.writePemFiles(keyDir);
                registry.add("jwt.private-key-location", () -> keyDir.resolve("private.pem").toUri().toString());
                registry.add("jwt.public-key-location", () -> keyDir.resolve("public.pem").toUri().toString());
        }

        /** Sends requests through the whole application, like a client would. */
        @Autowired
        private MockMvc mockMvc;

        /** Turns the request DTOs into JSON. */
        @Autowired
        private ObjectMapper objectMapper;

        /**
         * The real repository (no mock), used to reset the database and to count stored
         * events.
         */
        @Autowired
        private EventRepository eventRepository;

        /**
         * Used to reset the database, so every test can sign up the same user again.
         */
        @Autowired
        private UserRepository userRepository;

        /** Stores the two categories the events of a test refer to. */
        @Autowired
        private CategoryRepository categoryRepository;

        /** Id of the stored festival category, generated by the database; most tests use it. */
        private UUID categoryDefaultId;

        /** Id of the stored concert category, the second one the category filter needs. */
        private UUID categoryConcertId;

        /**
         * The H2 database lives as long as the Spring context, i.e. across tests.
         * Emptying it before every test keeps the tests independent of each other.
         * Events go first, because they reference the category.
         */
        @BeforeEach
        void setUpDatabase() {
                eventRepository.deleteAll();
                categoryRepository.deleteAll();
                userRepository.deleteAll();

                categoryDefaultId = categoryRepository.save(EventTestDataFactory
                                .createUnsavedCategory(EventTestDataFactory.DEFAULT_CATEGORY_TITLE)).getId();
                categoryConcertId = categoryRepository.save(EventTestDataFactory
                                .createUnsavedCategory(EventTestDataFactory.CONCERT_CATEGORY_TITLE)).getId();

        }

        @Test
        void shouldGetEventsWithDefaultPageableAndWithoutCategory() throws Exception {
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);
                List<Event> events = new ArrayList<>();

                for (CreateEventDto createEventDto : EventTestDataFactory.createCreateEventDtoList(categoryDefaultId)) {
                        events.add(createEvent(accessTk, createEventDto));
                }

                events.sort(Comparator.comparing(Event::getStartsAt));

                // @PageableDefault sorts by startsAt ascending, so title and startsAt are
                // compared in order (contains), the other fields as a set.
                mockMvc.perform(get("/event").header("Authorization", "Bearer " + accessTk))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(events.size())))
                                // generated by the database, so only checked for presence
                                .andExpect(jsonPath("$.content[*].id").value(everyItem(notNullValue())))
                                .andExpect(jsonPath("$.content[*].category.id").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getCategory().getId().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].title").value(contains(
                                                events.stream().map(Event::getTitle).toArray())))
                                .andExpect(jsonPath("$.content[*].description").value(containsInAnyOrder(
                                                events.stream().map(Event::getDescription).toArray())))
                                .andExpect(jsonPath("$.content[*].venue").value(containsInAnyOrder(
                                                events.stream().map(Event::getVenue).toArray())))
                                .andExpect(jsonPath("$.content[*].city").value(containsInAnyOrder(
                                                events.stream().map(Event::getCity).toArray())))
                                .andExpect(jsonPath("$.content[*].startsAt").value(contains(
                                                events.stream().map(e -> e.getStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].saleStartsAt").value(containsInAnyOrder(
                                                events.stream().map(e -> e.getSaleStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].imageUrl").value(containsInAnyOrder(
                                                events.stream().map(Event::getImageUrl).toArray())))
                                // created through POST, so every event starts as a draft
                                .andExpect(jsonPath("$.content[*].status").value(everyItem(is(Status.DRAFT.name()))))
                                // generated by the database, so only checked for presence
                                .andExpect(jsonPath("$.content[*].createdAt").value(everyItem(notNullValue())))
                                .andExpect(jsonPath("$.content[*].updatedAt").value(everyItem(notNullValue())))

                                .andExpect(jsonPath("$.page.size").value(EventTestDataFactory.DEFAULT_PAGE_SIZE))
                                .andExpect(jsonPath("$.page.number").value(EventTestDataFactory.DEFAULT_PAGE))
                                .andExpect(jsonPath("$.page.totalElements").value(events.size()))
                                .andExpect(jsonPath("$.page.totalPages").value(
                                                Math.ceilDiv(events.size(), EventTestDataFactory.DEFAULT_PAGE_SIZE)));
        }

        @Test
        void shouldGetEventsWithDefaultPageableAndWithCategory() throws Exception {
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);
                List<Event> eventsNotToAssert = new ArrayList<>();
                List<Event> eventsToAssert = new ArrayList<>();

                for (CreateEventDto createEventDto : EventTestDataFactory.createCreateEventDtoList(categoryDefaultId)) {
                        eventsNotToAssert.add(createEvent(accessTk, createEventDto));
                }

                for (CreateEventDto createEventDto : EventTestDataFactory.createCreateEventDtoList(categoryConcertId)) {
                        eventsToAssert.add(createEvent(accessTk, createEventDto));
                }

                eventsToAssert.sort(Comparator.comparing(Event::getStartsAt));

                // @PageableDefault sorts by startsAt ascending, so title and startsAt are
                // compared in order (contains), the other fields as a set.
                mockMvc.perform(get(String.format("/event?categoryId=%s", categoryConcertId))
                                .header("Authorization", "Bearer " + accessTk))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content", hasSize(eventsToAssert.size())))
                                // generated by the database, so only checked for presence
                                .andExpect(jsonPath("$.content[*].id").value(everyItem(notNullValue())))
                                // none of the events of the other category is returned
                                .andExpect(jsonPath("$.content[*].id").value(everyItem(not(in(
                                                eventsNotToAssert.stream().map(e -> e.getId().toString()).toArray())))))
                                .andExpect(jsonPath("$.content[*].category.id").value(containsInAnyOrder(
                                                eventsToAssert.stream().map(e -> e.getCategory().getId().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].category.id").value(everyItem(not(in(
                                                eventsNotToAssert.stream().map(e -> e.getCategory().getId().toString()).toArray())))))
                                .andExpect(jsonPath("$.content[*].title").value(contains(
                                                eventsToAssert.stream().map(Event::getTitle).toArray())))
                                .andExpect(jsonPath("$.content[*].description").value(containsInAnyOrder(
                                                eventsToAssert.stream().map(Event::getDescription).toArray())))
                                .andExpect(jsonPath("$.content[*].venue").value(containsInAnyOrder(
                                                eventsToAssert.stream().map(Event::getVenue).toArray())))
                                .andExpect(jsonPath("$.content[*].city").value(containsInAnyOrder(
                                                eventsToAssert.stream().map(Event::getCity).toArray())))
                                .andExpect(jsonPath("$.content[*].startsAt").value(contains(
                                                eventsToAssert.stream().map(e -> e.getStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].saleStartsAt").value(containsInAnyOrder(
                                                eventsToAssert.stream().map(e -> e.getSaleStartsAt().toString()).toArray())))
                                .andExpect(jsonPath("$.content[*].imageUrl").value(containsInAnyOrder(
                                                eventsToAssert.stream().map(Event::getImageUrl).toArray())))
                                // created through POST, so every event starts as a draft
                                .andExpect(jsonPath("$.content[*].status").value(everyItem(is(Status.DRAFT.name()))))
                                // generated by the database, so only checked for presence
                                .andExpect(jsonPath("$.content[*].createdAt").value(everyItem(notNullValue())))
                                .andExpect(jsonPath("$.content[*].updatedAt").value(everyItem(notNullValue())))

                                .andExpect(jsonPath("$.page.size").value(EventTestDataFactory.DEFAULT_PAGE_SIZE))
                                .andExpect(jsonPath("$.page.number").value(EventTestDataFactory.DEFAULT_PAGE))
                                .andExpect(jsonPath("$.page.totalElements").value(eventsToAssert.size()))
                                .andExpect(jsonPath("$.page.totalPages").value(
                                                Math.ceilDiv(eventsToAssert.size(), EventTestDataFactory.DEFAULT_PAGE_SIZE)));
        }

        @Test
        void shouldGetEvent() throws Exception {
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto(categoryDefaultId);

                Event event = createEvent(accessTk, createEventDto);

                mockMvc.perform(get("/event/{eventId}", event.getId())
                                .header("Authorization", "Bearer " + accessTk))
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
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto(categoryDefaultId);

                Event event = createEvent(accessTk, createEventDto);

                mockMvc.perform(get("/event/{eventId}", event.getId())
                                .header("Authorization", "Bearer " + accessTk))
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
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto(categoryDefaultId);

                Event event = createEvent(accessTk, createEventDto);

                mockMvc.perform(delete("/event/{eventId}", event.getId())
                                .header("Authorization", "Bearer " + accessTk))
                                .andExpect(status().isNoContent())
                                .andExpect(content().string(""));

                mockMvc.perform(get("/event/{eventId}", event.getId())
                                .header("Authorization", "Bearer " + accessTk))
                                .andExpect(status().isNotFound());
        }

        @Test
        void shouldUpdateEvent() throws Exception {
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto(categoryDefaultId);

                Event event = createEvent(accessTk, createEventDto);

                Event updatedEvent = EventTestDataFactory.createUpdatedEvent();
                UpdateEventDto updateEventDto = EventTestDataFactory.createUpdateEventDto(categoryDefaultId);

                String jsonRequest = objectMapper.writeValueAsString(updateEventDto);

                mockMvc.perform(patch("/event/{eventId}", event.getId())
                                .header("Authorization", "Bearer " + accessTk)
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
        void shouldNotCreateEventWithPastStartsAt() throws Exception {
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

                CreateEventDto createEventDto = EventTestDataFactory.createCreatePastEventDto();

                String jsonRequest = objectMapper.writeValueAsString(createEventDto);

                // @Future on startsAt in CreateEventDto rejects the payload
                mockMvc.perform(post("/event")
                                .header("Authorization", "Bearer " + accessTk)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest());
        }

        /**
         * A token that the resource server can't verify is rejected by the security
         * filter chain with 401, before the controller runs: nothing is stored.
         */
        @Test
        void shouldNotCreateEventWithInvalidAccessTk() throws Exception {
                String jsonRequest = objectMapper
                                .writeValueAsString(EventTestDataFactory.createCreateEventDto(categoryDefaultId));

                mockMvc.perform(post("/event")
                                .header("Authorization", "Bearer not-a-valid-token")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isUnauthorized());

                assertEquals(0, eventRepository.count());
        }

        @Test
        void shouldNotUpdateEventWithPastStartsAt() throws Exception {
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto(categoryDefaultId);

                Event event = createEvent(accessTk, createEventDto);

                UpdateEventDto updateEventDto = EventTestDataFactory.createUpdateEventDtoWithPastStartsAt();

                String jsonRequest = objectMapper.writeValueAsString(updateEventDto);

                mockMvc.perform(patch("/event/{eventId}", event.getId())
                                .header("Authorization", "Bearer " + accessTk)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(jsonRequest))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.status").value(400))
                                .andExpect(jsonPath("$.message").value("There is 1 error"))
                                .andExpect(jsonPath("$.errors[0].field").value("startsAt"))
                                .andExpect(jsonPath("$.errors[0].message")
                                                .value("The new start time must be in the future."));
        }

        @Test
        void shouldNotUpdateEventWithSaleStartingAfterEventStart() throws Exception {
                String accessTk = AuthTestClient.signUpAndReturnAccessTk(mockMvc, objectMapper);

                CreateEventDto createEventDto = EventTestDataFactory.createCreateEventDto(categoryDefaultId);

                Event event = createEvent(accessTk, createEventDto);

                UpdateEventDto updateEventDto = EventTestDataFactory.createUpdateEventDtoWithSaleAfterStart();

                String jsonRequest = objectMapper.writeValueAsString(updateEventDto);

                mockMvc.perform(patch("/event/{eventId}", event.getId())
                                .header("Authorization", "Bearer " + accessTk)
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
         * Creates the event through the API, expects 201 and returns the stored event
         * from the response.
         */
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
