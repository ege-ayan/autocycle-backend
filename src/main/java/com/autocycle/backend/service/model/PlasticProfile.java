package com.autocycle.backend.service.model;

import java.util.List;

import com.autocycle.backend.dto.ai.PlasticProfileDraft;
import com.autocycle.backend.entity.PlasticType;

public record PlasticProfile(
		String type,
		List<String> properties,
		List<String> applications,
		String environmentalImpact,
		String recyclingPotential,
		String regionalRelevance) {

	public PlasticProfile {
		properties = copy(properties);
		applications = copy(applications);
	}

	public static PlasticProfile from(PlasticType plastic) {
		return new PlasticProfile(
				plastic.getType(),
				plastic.getProperties(),
				plastic.getApplications(),
				plastic.getEnvironmentalImpact(),
				plastic.getRecyclingPotential(),
				plastic.getRegionalRelevance());
	}

	public static PlasticProfile fromDraft(String requestedType, PlasticProfileDraft draft) {
		return new PlasticProfile(
				requestedType,
				draft == null ? List.of() : draft.properties(),
				draft == null ? List.of() : draft.applications(),
				draft == null ? null : draft.environmentalImpact(),
				draft == null ? null : draft.recyclingPotential(),
				draft == null ? null : draft.regionalRelevance());
	}

	public PlasticProfile withType(String requestedType) {
		return new PlasticProfile(requestedType, properties, applications, environmentalImpact, recyclingPotential, regionalRelevance);
	}

	private static List<String> copy(List<String> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return List.copyOf(values.stream().filter(value -> value != null && !value.isBlank()).toList());
	}
}
