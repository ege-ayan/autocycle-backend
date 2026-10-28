package com.autocycle.backend.dto.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CollaborationDraft(
		@JsonProperty("input_summary") InputSummary inputSummary,
		@JsonProperty("top_collaborations") List<Recommendation> topCollaborations) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record InputSummary(String brand, String plastic) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Recommendation(
			@JsonProperty("brand_name") String brandName,
			@JsonProperty("brand_placement") List<String> brandPlacement,
			@JsonProperty("sustainability_placement") String sustainabilityPlacement,
			@JsonProperty("product_assumptions") List<String> productAssumptions,
			@JsonProperty("collaboration_summary") String collaborationSummary,
			@JsonProperty("estimated_impact") String estimatedImpact,
			@JsonProperty("confidence_score") Integer confidenceScore,
			@JsonProperty("combined_reach") String combinedReach) {
	}
}
