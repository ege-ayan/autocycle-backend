package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autocycle.backend.dto.requests.GenerateProductsRequest;
import com.autocycle.backend.dto.responses.ProductResponse;
import com.autocycle.backend.exception.GenerationException;
import com.autocycle.backend.service.model.BrandProfile;
import com.autocycle.backend.service.model.PlasticProfile;
import com.autocycle.backend.service.model.StoredCollaboration;
import com.autocycle.backend.service.model.StoredProduct;
import com.autocycle.backend.util.JsonDocuments;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ProductGenerationServiceTest {

	@Mock
	private CollaborationService collaborations;
	@Mock
	private ProfileStore profiles;
	@Mock
	private AiClient aiClient;
	@Mock
	private WebSearchClient webSearch;
	@Mock
	private ProductImageService images;

	private ProductGenerationService service;

	@BeforeEach
	void setUp() {
		service = new ProductGenerationService(
				collaborations, profiles, aiClient, webSearch, images,
				new JsonDocuments(JsonMapper.builder().build()), Runnable::run);
	}

	@Test
	void returnsStoredProductsWithoutCallingTheModel() {
		when(collaborations.find("BMW", "Adidas", "PET", "Munich")).thenReturn(Optional.of(stored()));

		ProductResponse response = service.generate(new GenerateProductsRequest("BMW", "PET", "Munich", "Adidas"));

		assertThat(response.cached()).isTrue();
		assertThat(response.products()).hasSize(1);
		assertThat(response.sourceBrand()).isEqualTo("BMW");
		assertThat(response.products().getFirst().imageUrl()).isEqualTo("https://cdn.test/shoe.png");
		verify(aiClient, never()).complete(anyString(), anyString(), org.mockito.ArgumentMatchers.anyBoolean());
	}

	@Test
	void generatesAndStoresAProductWhenNothingIsCached() {
		when(collaborations.find(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
		when(profiles.findKnownBrand(anyString()))
				.thenReturn(Optional.of(brand("BMW")))
				.thenReturn(Optional.of(brand("Adidas")));
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.of(plastic()));
		when(aiClient.complete(eq(Prompts.PRODUCT_IDEA_SYSTEM), anyString(), eq(true))).thenReturn(ideaJson());
		when(aiClient.complete(eq(Prompts.PITCH_SYSTEM), anyString(), eq(false))).thenReturn("**Concept:** A recycled sneaker.");
		when(images.create(anyString(), eq("Ocean Runner"))).thenReturn("https://cdn.test/generated.png");
		when(collaborations.createOrGet("BMW", "Adidas", "PET", "Munich")).thenReturn(9L);

		ProductResponse response = service.generate(new GenerateProductsRequest("BMW", "PET", "Munich", "Adidas"));

		assertThat(response.cached()).isFalse();
		assertThat(response.products().getFirst().pitch()).contains("Concept");
		verify(collaborations).addProduct(eq(9L), any(StoredProduct.class), org.mockito.ArgumentMatchers.anyMap());
	}

	@Test
	void keepsThePitchWhenImageUploadFails() {
		when(collaborations.find(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
		when(profiles.findKnownBrand(anyString()))
				.thenReturn(Optional.of(brand("BMW")))
				.thenReturn(Optional.of(brand("Adidas")));
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.of(plastic()));
		when(aiClient.complete(eq(Prompts.PRODUCT_IDEA_SYSTEM), anyString(), eq(true))).thenReturn(ideaJson());
		when(aiClient.complete(eq(Prompts.PITCH_SYSTEM), anyString(), eq(false))).thenReturn("pitch");
		when(images.create(anyString(), anyString())).thenThrow(new IllegalStateException("bucket missing"));
		when(collaborations.createOrGet(anyString(), anyString(), anyString(), anyString())).thenReturn(3L);

		ProductResponse response = service.generate(new GenerateProductsRequest("BMW", "PET", "Munich", "Adidas"));

		assertThat(response.products().getFirst().pitch()).isEqualTo("pitch");
		assertThat(response.products().getFirst().imageUrl()).isNull();
		verify(collaborations).addProduct(anyLong(), any(StoredProduct.class), org.mockito.ArgumentMatchers.anyMap());
	}

	@Test
	void rejectsAnEmptyProductList() {
		when(collaborations.find(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
		when(profiles.findKnownBrand(anyString())).thenReturn(Optional.of(brand("BMW")));
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.of(plastic()));
		when(aiClient.complete(eq(Prompts.PRODUCT_IDEA_SYSTEM), anyString(), eq(true))).thenReturn("{\"products\":[]}");

		assertThatThrownBy(() -> service.generate(new GenerateProductsRequest("BMW", "PET", "Munich", "Adidas")))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("product concept");
		verify(aiClient, never()).complete(eq(Prompts.PITCH_SYSTEM), anyString(), eq(false));
	}

	@Test
	void rejectsAPitchFailure() {
		when(collaborations.find(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
		when(profiles.findKnownBrand(anyString())).thenReturn(Optional.of(brand("BMW")));
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.of(plastic()));
		when(aiClient.complete(eq(Prompts.PRODUCT_IDEA_SYSTEM), anyString(), eq(true))).thenReturn(ideaJson());
		when(aiClient.complete(eq(Prompts.PITCH_SYSTEM), anyString(), eq(false)))
				.thenThrow(new GenerationException("pitch failed"));
		when(collaborations.createOrGet(anyString(), anyString(), anyString(), anyString())).thenReturn(3L);

		assertThatThrownBy(() -> service.generate(new GenerateProductsRequest("BMW", "PET", "Munich", "Adidas")))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("pitch failed");
		verify(collaborations, never()).addProduct(anyLong(), any(StoredProduct.class), org.mockito.ArgumentMatchers.anyMap());
	}

	private static StoredCollaboration stored() {
		return new StoredCollaboration(1L, "bmw", "adidas", "pet", "munich", List.of(
				new StoredProduct("Ocean Runner", "sneaker", "A recycled shoe.", "pitch", "https://cdn.test/shoe.png")));
	}

	private static BrandProfile brand(String name) {
		return new BrandProfile(name, "automotive", List.of(), List.of(), "", List.of(), List.of(), "");
	}

	private static PlasticProfile plastic() {
		return new PlasticProfile("PET", List.of("clear"), List.of("bottles"), "", "", "");
	}

	private static String ideaJson() {
		return """
				{"input_summary":{"brand_1":"BMW","brand_2":"Adidas","plastic_material":"PET"},
				"products":[{"product_type":"sneaker","name":"Ocean Runner","description":"A recycled performance sneaker."}]}
				""";
	}
}
