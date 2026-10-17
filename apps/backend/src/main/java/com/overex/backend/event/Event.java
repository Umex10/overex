package com.overex.backend.event;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "events")
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Event {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false, length = 120)
	@NotBlank
	@Size(max = 120)
	private String title;


	@Column(nullable = false, columnDefinition = "text")
	@NotBlank
	private String description;

	@Column(nullable = false, length = 120)
	@NotBlank
	@Size(max = 120)
	private String venue;

	@Column(nullable = false, length = 80)
	@NotBlank
	@Size(max = 80)
	private String city;

	@Column(nullable = false)
	@NotNull
	private Instant startsAt;

	@Column(nullable = false)
	@NotNull
	private Instant saleStartsAt;

	@Column(nullable = false, length = 500)
	@NotBlank
	@Size(max = 500)
	private String imageUrl;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	@NotNull
	private Status status;

	@Column(nullable = false, updatable = false)
	@CreationTimestamp
	private Instant createdAt;

	@Column(nullable = false)
	@UpdateTimestamp
	private Instant updatedAt;
}
