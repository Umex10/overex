package com.overex.backend.event;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.overex.backend.event.dto.responses.CountsOfEventsDto;
import com.overex.backend.event.dto.responses.EventResponseDto;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * The signed-in user's own view of events: the events they created, drafts
 * included.
 *
 * <p>
 * Every route here requires a valid access token ({@code /me/**} is not in
 * the public list of {@code SecurityConfig}). The user id always comes from the
 * verified token, never from the URL, so nobody can ask for another user's
 * events.
 */
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/me/events")
public class MyEventsController {

    private final EventService eventService;

    /**
     * {@code GET /me/events}: one page of the signed-in user's events.
     *
     * @param jwt      the verified access token; its subject is the user id
     * @param filter   which tab: a status, FINISHED (published and already started) or ALL (default)
     * @param pageable page, size and sort from the query string
     */
    @GetMapping
    public ResponseEntity<Page<EventResponseDto>> getMyEvents(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(defaultValue = "ALL") MyEventsFilter filter,
            @PageableDefault(size = 10, sort = "startsAt", direction = Sort.Direction.ASC) Pageable pageable) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Page<EventResponseDto> events = eventService.getMyEvents(userId, categoryId, filter, pageable);
        return new ResponseEntity<Page<EventResponseDto>>(events, HttpStatus.OK);
    }

    @GetMapping("/counts")
    ResponseEntity<CountsOfEventsDto> getCountsOfEvents(@AuthenticationPrincipal Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());

        CountsOfEventsDto countEventDto = eventService.getCountsOfEvents(userId);

        return new ResponseEntity<CountsOfEventsDto>(countEventDto, HttpStatus.OK);

    }

}
