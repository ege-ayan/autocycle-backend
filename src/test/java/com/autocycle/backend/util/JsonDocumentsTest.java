package com.autocycle.backend.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.autocycle.backend.dto.ai.BrandProfileDraft;
import com.autocycle.backend.exception.GenerationException;

import tools.jackson.databind.json.JsonMapper;

class JsonDocumentsTest {

	private final JsonDocuments documents = new JsonDocuments(JsonMapper.builder().build());

	@Test
	void readsAFencedJsonObject() {
		BrandProfileDraft draft = documents.read("""
				```json
				{"name":"BMW","industry":"automotive","product_categories":["cars"]}
				```
				""", BrandProfileDraft.class);

		assertThat(draft.industry()).isEqualTo("automotive");
		assertThat(draft.productCategories()).containsExactly("cars");
	}

	@Test
	void rejectsTextThatIsNotJson() {
		assertThatThrownBy(() -> documents.read("the model explained it in prose", BrandProfileDraft.class))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("invalid JSON");
	}
}
