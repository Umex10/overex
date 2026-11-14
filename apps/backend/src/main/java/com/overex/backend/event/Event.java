package com.overex.backend.event;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.overex.backend.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
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

	/**
	 * Optimistic-locking counter. Hibernate sets it to 0 on insert and raises it
	 * by one on every update, so a client that sends an older value edited an
	 * outdated state (see {@code EventService#updateEvent}).
	 */
	@Version
	private Long version;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id", nullable = false)
	private Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private User createdBy;

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
