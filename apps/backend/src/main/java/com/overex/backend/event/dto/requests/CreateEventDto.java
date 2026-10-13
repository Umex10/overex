package com.overex.backend.event.dto.requests;

import java.time.Instant;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateEventDto(

		@NotBlank(message = "Title is required")
		@Size(max = 120, message = "Title must be at most {max} characters.")
		String title,

		@NotBlank(message = "Description is required")
		String description,

		@NotBlank(message = "Venue is required")
		@Size(max = 120, message = "Venue must be at most {max} characters.")
		String venue,

		@NotBlank(message = "City is required")
		@Size(max = 80, message = "City must be at most {max} characters.")
		String city,

		@NotNull(message = "Start time is required")
		@Future(message = "Start time must be in the future.")
		Instant startsAt,

		@NotNull(message = "Sale start is required")
		Instant saleStartsAt,

		@NotBlank(message = "Image URL is required")
		@Size(max = 500, message = "Image URL must be at most {max} characters.")
		@URL(message = "Please enter a valid image URL.")
		String imageUrl) {
}
