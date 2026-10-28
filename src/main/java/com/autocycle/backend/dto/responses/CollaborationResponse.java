package com.autocycle.backend.dto.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record CollaborationResponse(
		String brand,
		String plasticType,
		String location,
		boolean needPlastic,
		List<Partner> partners) {

	public record Partner(
			String brandName,
			List<String> brandPlacement,
			String sustainabilityPlacement,
			List<String> productAssumptions,
			String collaborationSummary,
			String estimatedImpact,
			Integer confidenceScore,
			String combinedReach,
			String logoUrl,
			String companyDomain) {
	}
}
