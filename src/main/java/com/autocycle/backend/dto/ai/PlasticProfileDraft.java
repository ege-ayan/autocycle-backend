package com.autocycle.backend.dto.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PlasticProfileDraft(
		String type,
		@JsonProperty("type_name") String typeName,
		List<String> properties,
		List<String> applications,
		@JsonProperty("environmental_impact") String environmentalImpact,
		@JsonProperty("recycling_potential") String recyclingPotential,
		@JsonProperty("regional_relevance") String regionalRelevance) {
}
