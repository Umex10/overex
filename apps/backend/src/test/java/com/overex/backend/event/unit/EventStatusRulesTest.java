package com.overex.backend.event.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.overex.backend.event.Event;
import com.overex.backend.event.EventStatusRules;
import com.overex.backend.event.Status;
import com.overex.backend.event.dto.responses.StatusOption;
import com.overex.backend.event.utils.EventTestDataFactory;
import com.overex.backend.event.utils.EventTestDataFactory.StatusRuleCase;

/**
 * Unit tests for {@link EventStatusRules}: every rule case, and the exact moment
 * of the start (or of the sale start), which already counts as started.
 *
 * <p>
 * {@code now} is passed in, so every test uses a fixed point in time. That the
 * service applies the rules is tested in {@link EventServiceTest}.
 */
class EventStatusRulesTest {

	private static final Instant NOW = EventTestDataFactory.DEFAULT_STARTS_AT;

	// Between the past (2020) and future (2099) dates of the rule cases.
	private static final Instant BETWEEN_PAST_AND_FUTURE = Instant.parse("2026-01-01T12:00:00Z");

	/** Every stored status and point in time gets the expected status options. */
	@Test
	void shouldGetStatusOptionsForEveryRuleCase() {
		for (StatusRuleCase ruleCase : EventTestDataFactory.createStatusRuleCases()) {
			// Act
			Map<Status, StatusOption> options = EventStatusRules.statusOptions(ruleCase.event(),
					BETWEEN_PAST_AND_FUTURE);

			// Assert
			assertEquals(ruleCase.expectedOptions(), options, ruleCase.name());
		}
	}

	/** {@code check} answers every single status change like the options. */
	@Test
	void shouldCheckEveryStatusChangeOfEveryRuleCase() {
		for (StatusRuleCase ruleCase : EventTestDataFactory.createStatusRuleCases()) {
			for (Status to : Status.values()) {
				// Act
				StatusOption option = EventStatusRules.check(ruleCase.event(), to, BETWEEN_PAST_AND_FUTURE);

				// Assert
				assertEquals(ruleCase.expectedOptions().get(to), option, ruleCase.name() + " → " + to);
			}
		}
	}

	@Test
	void shouldBlockPublishingDraftAtExactStart() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		event.setStatus(Status.DRAFT);
		event.setStartsAt(NOW);

		// Act
		StatusOption option = EventStatusRules.check(event, Status.PUBLISHED, NOW);

		// Assert
		assertEquals(StatusOption.blocked(EventTestDataFactory.STARTED_PUBLISH_BLOCKED_WHY), option);
	}

	@Test
	void shouldBlockCancelingPublishedEventAtExactStart() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		event.setStatus(Status.PUBLISHED);
		event.setStartsAt(NOW);

		// Act
		StatusOption option = EventStatusRules.check(event, Status.CANCELED, NOW);

		// Assert
		assertEquals(StatusOption.blocked(EventTestDataFactory.STARTED_CANCEL_BLOCKED_WHY), option);
	}

	@Test
	void shouldBlockPublishedEventGoingBackToDraftAtExactSaleStart() {
		// Arrange
		Event event = EventTestDataFactory.createDefaultEvent();
		event.setStatus(Status.PUBLISHED);
		event.setSaleStartsAt(NOW);
		event.setStartsAt(NOW.plusSeconds(3600));

		// Act
		StatusOption option = EventStatusRules.check(event, Status.DRAFT, NOW);

		// Assert
		assertEquals(StatusOption.blocked(EventTestDataFactory.SALE_STARTED_DRAFT_BLOCKED_WHY), option);
	}

}
