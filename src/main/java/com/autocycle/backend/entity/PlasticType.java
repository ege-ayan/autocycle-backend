package com.autocycle.backend.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "plastic_types")
@Getter
@Setter
@NoArgsConstructor
public class PlasticType {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "plastic_id", nullable = false, unique = true, length = 64)
	private String plasticId;

	@Column(nullable = false)
	private String type;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private List<String> properties = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(columnDefinition = "jsonb")
	private List<String> applications = new ArrayList<>();

	@Column(name = "environmental_impact", columnDefinition = "text")
	private String environmentalImpact;

	@Column(name = "recycling_potential", columnDefinition = "text")
	private String recyclingPotential;

	@Column(name = "regional_relevance", columnDefinition = "text")
	private String regionalRelevance;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
