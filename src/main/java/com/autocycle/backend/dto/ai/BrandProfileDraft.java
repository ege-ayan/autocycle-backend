package com.autocycle.backend.dto.ai;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BrandProfileDraft(
		String name,
		String brand,
		String industry,
		@JsonProperty("product_categories") List<String> productCategories,
		@JsonProperty("plastic_materials") List<String> plasticMaterials,
		@JsonProperty("sustainability_philosophy") String sustainabilityPhilosophy,
		@JsonProperty("key_partners_or_collaborators") List<String> keyPartnersOrCollaborators,
		@JsonProperty("manufacturing_regions") List<String> manufacturingRegions,
		@JsonProperty("brand_positioning") String brandPositioning) {
}
