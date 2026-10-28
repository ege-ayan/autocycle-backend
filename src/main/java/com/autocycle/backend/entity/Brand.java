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
@Table(name = "brands")
@Getter
@Setter
@NoArgsConstructor
public class Brand {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "brand_id", nullable = false, unique = true, length = 64)
	private String brandId;

	@Column(nullable = false)
	private String name;

	private String industry;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "product_categories", columnDefinition = "jsonb")
	private List<String> productCategories = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "plastic_materials", columnDefinition = "jsonb")
	private List<String> plasticMaterials = new ArrayList<>();

	@Column(name = "sustainability_philosophy", columnDefinition = "text")
	private String sustainabilityPhilosophy;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "key_partners_or_collaborators", columnDefinition = "jsonb")
	private List<String> keyPartnersOrCollaborators = new ArrayList<>();

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "manufacturing_regions", columnDefinition = "jsonb")
	private List<String> manufacturingRegions = new ArrayList<>();

	@Column(name = "brand_positioning", columnDefinition = "text")
	private String brandPositioning;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;
}
