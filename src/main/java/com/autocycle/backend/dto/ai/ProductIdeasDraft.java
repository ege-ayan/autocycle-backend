package com.autocycle.backend.dto.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProductIdeasDraft(
		@JsonProperty("input_summary") InputSummary inputSummary,
		List<ProductIdea> products) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InputSummary(
			@JsonProperty("brand_1") String brand1,
			@JsonProperty("brand_2") String brand2,
			@JsonProperty("plastic_material") String plasticMaterial) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record ProductIdea(
			@JsonProperty("product_type") String productType,
			String name,
			String description) {
	}
}
