package com.autocycle.backend.service.model;

import java.util.ArrayList;
import java.util.List;

import com.autocycle.backend.dto.ai.BrandProfileDraft;
import com.autocycle.backend.entity.Brand;

public record BrandProfile(
		String name,
		String industry,
		List<String> productCategories,
		List<String> plasticMaterials,
		String sustainabilityPhilosophy,
		List<String> keyPartnersOrCollaborators,
		List<String> manufacturingRegions,
		String brandPositioning) {

	public BrandProfile {
		productCategories = copy(productCategories);
		plasticMaterials = copy(plasticMaterials);
		keyPartnersOrCollaborators = copy(keyPartnersOrCollaborators);
		manufacturingRegions = copy(manufacturingRegions);
	}

	public static BrandProfile from(Brand brand) {
		return new BrandProfile(
				brand.getName(),
				brand.getIndustry(),
				brand.getProductCategories(),
				brand.getPlasticMaterials(),
				brand.getSustainabilityPhilosophy(),
				brand.getKeyPartnersOrCollaborators(),
				brand.getManufacturingRegions(),
				brand.getBrandPositioning());
	}

	public static BrandProfile fromDraft(String requestedName, BrandProfileDraft draft) {
		return new BrandProfile(
				requestedName,
				draft == null ? null : draft.industry(),
				draft == null ? List.of() : draft.productCategories(),
				draft == null ? List.of() : draft.plasticMaterials(),
				draft == null ? null : draft.sustainabilityPhilosophy(),
				draft == null ? List.of() : draft.keyPartnersOrCollaborators(),
				draft == null ? List.of() : draft.manufacturingRegions(),
				draft == null ? null : draft.brandPositioning());
	}

	public BrandProfile withName(String requestedName) {
		return new BrandProfile(
				requestedName,
				industry,
				productCategories,
				plasticMaterials,
				sustainabilityPhilosophy,
				keyPartnersOrCollaborators,
				manufacturingRegions,
				brandPositioning);
	}

	private static List<String> copy(List<String> values) {
		if (values == null || values.isEmpty()) {
			return List.of();
		}
		return List.copyOf(values.stream().filter(value -> value != null && !value.isBlank()).toList());
	}

	public List<String> mutable(List<String> values) {
		return new ArrayList<>(values);
	}
}
