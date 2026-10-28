package com.autocycle.backend.util;

import com.autocycle.backend.service.model.BrandProfile;
import com.autocycle.backend.service.model.PlasticProfile;

public final class EmbeddingText {

	private EmbeddingText() {
	}

	public static String brand(BrandProfile profile) {
		return """
				%s is a brand in the %s industry.
				It primarily offers products such as: %s.
				The brand uses the following plastic materials: %s.
				Sustainability philosophy: %s.
				Notable collaborations and partners include: %s.
				Manufacturing and operational regions: %s.
				Its brand positioning is: %s.""".formatted(
				profile.name(),
				blank(profile.industry(), "unspecified"),
				join(profile.productCategories()),
				join(profile.plasticMaterials()),
				blank(profile.sustainabilityPhilosophy(), "Not specified"),
				join(profile.keyPartnersOrCollaborators()),
				join(profile.manufacturingRegions()),
				blank(profile.brandPositioning(), "Not specified"))
				.toLowerCase();
	}

	public static String brandQuery(String query) {
		String name = query.toLowerCase();
		return """
				%s is a global brand in its respective industry.
				It offers products or services aligned with its market positioning.
				It may use or recycle plastic materials in its operations.
				It could be engaged in sustainability initiatives or ESG practices.
				It may collaborate with other brands or suppliers.
				It operates in specific regional or global markets.
				Its brand positioning may be premium, eco-conscious, or mainstream.""".formatted(name);
	}

	public static String plastic(PlasticProfile profile) {
		return """
				Plastic Type: %s
				Properties: %s
				Applications: %s
				Environmental Impact: %s
				Recycling Potential: %s
				Regional Relevance: %s""".formatted(
				profile.type(),
				join(profile.properties()),
				join(profile.applications()),
				blank(profile.environmentalImpact(), ""),
				blank(profile.recyclingPotential(), ""),
				blank(profile.regionalRelevance(), ""))
				.toLowerCase();
	}

	public static String plasticQuery(String query) {
		return ("Plastic type: " + query).toLowerCase();
	}

	private static String join(java.util.List<String> values) {
		return values == null || values.isEmpty() ? "" : String.join(", ", values);
	}

	private static String blank(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}
}
