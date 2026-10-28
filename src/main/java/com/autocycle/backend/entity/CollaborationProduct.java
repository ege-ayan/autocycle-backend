package com.autocycle.backend.entity;

import java.util.LinkedHashMap;
import java.util.Map;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "collaboration_products")
@Getter
@Setter
@NoArgsConstructor
public class CollaborationProduct {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "collaboration_id", nullable = false)
	private Collaboration collaboration;

	@Column(name = "product_type")
	private String productType;

	@Column(name = "product_name")
	private String productName;

	@Column(name = "product_description", columnDefinition = "text")
	private String productDescription;

	@Column(columnDefinition = "text")
	private String pitch;

	@Column(name = "image_url", columnDefinition = "text")
	private String imageUrl;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "full_result", columnDefinition = "jsonb")
	private Map<String, Object> fullResult = new LinkedHashMap<>();
}
