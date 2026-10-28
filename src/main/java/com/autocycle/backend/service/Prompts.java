package com.autocycle.backend.service;

final class Prompts {

	static final String BRAND_SYSTEM = """
			You are a brand similarity research expert. Extract a structured brand profile for semantic matching.
			Use only the supplied evidence. Do not invent partners, materials, or certifications.
			When evidence is missing, leave lists empty and text fields as an empty string.
			Return a single JSON object with these keys:
			name, industry, product_categories, plastic_materials, sustainability_philosophy,
			key_partners_or_collaborators, manufacturing_regions, brand_positioning.
			""";

	static final String PLASTIC_SYSTEM = """
			You are a polymer expert helping a sustainability initiative.
			Summarize the plastic from the supplied evidence only.
			Return a single JSON object with these keys:
			type, properties, applications, environmental_impact, recycling_potential, regional_relevance.
			properties and applications are arrays of short strings.
			""";

	static final String COLLABORATION_SYSTEM = """
			You are a cross-industry collaboration strategist. Design physical, manufacturable product collaborations
			that use sustainable plastic as a functional material, not decoration.
			Return a single JSON object with input_summary {brand, plastic} and top_collaborations.
			Each collaboration has brand_name, brand_placement (2-3 strings), sustainability_placement,
			product_assumptions (2-4 physical product types), collaboration_summary,
			estimated_impact (string such as 75M), confidence_score (integer 1-100), combined_reach (string such as 50M).
			Return at most 5 collaborations. Do not include logo fields.
			""";

	static final String PRODUCT_IDEA_SYSTEM = """
			You are an AI-driven brand synergy strategist. Generate exactly one manufacturable collaborative product.
			The plastic must shape function, texture, or the sustainability story.
			Return a single JSON object:
			{"input_summary": {"brand_1": "", "brand_2": "", "plastic_material": ""},
			 "products": [{"product_type": "", "name": "", "description": ""}]}
			The description is 40-50 words.
			""";

	static final String PITCH_SYSTEM = """
			You are a creative pitch writer for sustainable co-branded products.
			Write markdown with these headings: Concept, Leveraging Brand Identity, Market Positioning and Intended Audience,
			Sustainability Innovation, Co-Branding and Storytelling Potential.
			Keep the concept to 10-20 words and the full pitch concrete.
			""";

	private Prompts() {
	}

	static String brandUser(String brand, String location, String evidence) {
		return """
				Brand: %s
				Location preference: %s
				Evidence from one web search:
				%s
				""".formatted(brand, blank(location), blank(evidence));
	}

	static String plasticUser(String plasticType, String location, String evidence) {
		return """
				Plastic type: %s
				Location: %s
				Evidence from one web search:
				%s
				""".formatted(plasticType, blank(location), blank(evidence));
	}

	static String collaborationUser(String brandJson, String plasticJson, String location, boolean needPlastic) {
		String direction = needPlastic
				? "The brand NEEDS sustainable plastic. Recommend partners that can supply, upcycle, or integrate their own plastic waste."
				: "The brand PROVIDES sustainable plastic. Recommend partners that can use this plastic in their own product line.";
		return """
				Brand profile JSON:
				%s

				Plastic profile JSON:
				%s

				Location: %s
				need_plastic: %s
				%s
				""".formatted(brandJson, plasticJson, blank(location), needPlastic, direction);
	}

	static String productIdeaUser(String sourceJson, String targetJson, String plasticJson) {
		return """
				Source brand contributes the material and its identity:
				%s

				Target brand contributes the product category and audience:
				%s

				Plastic material:
				%s
				""".formatted(sourceJson, targetJson, plasticJson);
	}

	static String pitchUser(String source, String target, String productType, String productName, String description) {
		return """
				Write the pitch for an upcycled %s named "%s", a collaboration between %s and %s.
				Product description: %s
				""".formatted(blank(productType), productName, source, target, blank(description));
	}

	static String imagePrompt(String source, String target, String productType, String productName) {
		return """
				Create a high-quality product image of the %s named "%s".
				Visualize a collaboration between %s and %s.
				Realistic editorial style, modern commercial tones, soft lighting, natural shadows.
				Include the text "%s X %s" and no logos.
				""".formatted(blank(productType), productName, source, target, source, target);
	}

	private static String blank(String value) {
		return value == null || value.isBlank() ? "unspecified" : value;
	}
}
