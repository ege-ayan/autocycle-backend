package com.autocycle.backend.entity;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "collaborations")
@Getter
@Setter
@NoArgsConstructor
public class Collaboration {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "source_brand", nullable = false)
	private String sourceBrand;

	@Column(name = "target_brand", nullable = false)
	private String targetBrand;

	@Column(name = "plastic_type")
	private String plasticType;

	private String location;

	@CreationTimestamp
	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@OneToMany(mappedBy = "collaboration", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("id ASC")
	private List<CollaborationProduct> products = new ArrayList<>();
}
