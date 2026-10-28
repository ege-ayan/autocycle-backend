package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.autocycle.backend.dto.requests.GenerateBrandRequest;
import com.autocycle.backend.dto.responses.CollaborationResponse;
import com.autocycle.backend.exception.GenerationException;
import com.autocycle.backend.exception.InvalidRequestException;
import com.autocycle.backend.service.model.BrandProfile;
import com.autocycle.backend.service.model.PlasticProfile;
import com.autocycle.backend.util.JsonDocuments;

import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class BrandResearchServiceTest {

	@Mock
	private ProfileStore profiles;
	@Mock
	private AiClient aiClient;
	@Mock
	private WebSearchClient webSearch;
	@Mock
	private LogoClient logos;

	private BrandResearchService service;

	@BeforeEach
	void setUp() {
		Executor direct = Runnable::run;
		service = new BrandResearchService(profiles, aiClient, webSearch, logos,
				new JsonDocuments(JsonMapper.builder().build()), direct);
	}

	@Test
	void reusesStoredProfilesAndStillAsksForCollaborations() {
		when(profiles.findKnownBrand("BMW")).thenReturn(Optional.of(brand()));
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.of(plastic()));
		when(aiClient.complete(eq(Prompts.COLLABORATION_SYSTEM), anyString(), eq(true))).thenReturn(collaborationJson());
		when(logos.find("Adidas")).thenReturn(Optional.of(new LogoClient.Logo("https://logo.test/adidas.png", "adidas.com")));

		CollaborationResponse response = service.generate(new GenerateBrandRequest("BMW", "PET", "Munich", false));

		assertThat(response.brand()).isEqualTo("BMW");
		assertThat(response.partners()).hasSize(1);
		assertThat(response.partners().getFirst().logoUrl()).isEqualTo("https://logo.test/adidas.png");
		assertThat(response.partners().getFirst().companyDomain()).isEqualTo("adidas.com");
		verify(aiClient, never()).complete(eq(Prompts.BRAND_SYSTEM), anyString(), anyBoolean());
		verify(profiles, never()).rememberBrand(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void researchesUnknownProfilesAndStoresThem() {
		when(profiles.findKnownBrand("BMW")).thenReturn(Optional.empty());
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.empty());
		when(webSearch.search(anyString())).thenReturn("public sustainability page");
		when(aiClient.complete(eq(Prompts.BRAND_SYSTEM), anyString(), eq(true))).thenReturn(brandJson());
		when(aiClient.complete(eq(Prompts.PLASTIC_SYSTEM), anyString(), eq(true))).thenReturn(plasticJson());
		when(aiClient.complete(eq(Prompts.COLLABORATION_SYSTEM), anyString(), eq(true))).thenReturn(collaborationJson());
		when(logos.find(anyString())).thenReturn(Optional.empty());

		CollaborationResponse response = service.generate(new GenerateBrandRequest("BMW", "PET", "Munich", true));

		assertThat(response.partners().getFirst().brandName()).isEqualTo("Adidas");
		verify(profiles).rememberBrand(org.mockito.ArgumentMatchers.any());
		verify(profiles).rememberPlastic(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void researchesABrandWithoutAPlasticType() {
		when(profiles.findKnownBrand("BMW")).thenReturn(Optional.of(brand()));
		when(aiClient.complete(eq(Prompts.COLLABORATION_SYSTEM), anyString(), eq(true))).thenReturn(collaborationJson());
		when(logos.find("Adidas")).thenReturn(Optional.empty());

		CollaborationResponse response = service.generate(new GenerateBrandRequest("BMW", " ", "Munich", false));

		assertThat(response.brand()).isEqualTo("BMW");
		assertThat(response.plasticType()).isBlank();
		assertThat(response.partners()).hasSize(1);
		verify(profiles, never()).findKnownPlastic(anyString());
	}

	@Test
	void researchesAPlasticWithoutABrand() {
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.of(plastic()));
		when(aiClient.complete(eq(Prompts.COLLABORATION_SYSTEM), anyString(), eq(true))).thenReturn(collaborationJson());
		when(logos.find("Adidas")).thenReturn(Optional.empty());

		CollaborationResponse response = service.generate(new GenerateBrandRequest(null, "PET", "Munich", true));

		assertThat(response.brand()).isBlank();
		assertThat(response.plasticType()).isEqualTo("PET");
		verify(profiles, never()).findKnownBrand(anyString());
	}

	@Test
	void rejectsInvalidModelJson() {
		when(profiles.findKnownBrand("BMW")).thenReturn(Optional.of(brand()));
		when(profiles.findKnownPlastic("PET")).thenReturn(Optional.of(plastic()));
		when(aiClient.complete(eq(Prompts.COLLABORATION_SYSTEM), anyString(), eq(true))).thenReturn("not json");

		assertThatThrownBy(() -> service.generate(new GenerateBrandRequest("BMW", "PET", "Munich", false)))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("invalid JSON");
	}

	@Test
	void rejectsAnEmptyRequest() {
		assertThatThrownBy(() -> service.generate(new GenerateBrandRequest(" ", " ", "Munich", false)))
				.isInstanceOf(InvalidRequestException.class)
				.hasMessageContaining("brand or plastic");
	}

	private static BrandProfile brand() {
		return new BrandProfile("BMW", "automotive", List.of("cars"), List.of("PP"), "circular", List.of(), List.of("Europe"), "premium");
	}

	private static PlasticProfile plastic() {
		return new PlasticProfile("PET", List.of("clear"), List.of("bottles"), "persistent", "high", "Europe");
	}

	private static String brandJson() {
		return """
				{"name":"BMW","industry":"automotive","product_categories":["cars"],"plastic_materials":["PP"],
				"sustainability_philosophy":"circular","key_partners_or_collaborators":[],"manufacturing_regions":["Europe"],
				"brand_positioning":"premium"}
				""";
	}

	private static String plasticJson() {
		return """
				{"type":"PET","properties":["clear"],"applications":["bottles"],"environmental_impact":"persistent",
				"recycling_potential":"high","regional_relevance":"Europe"}
				""";
	}

	private static String collaborationJson() {
		return """
				{"input_summary":{"brand":"BMW","plastic":"PET"},"top_collaborations":[{
				  "brand_name":"Adidas","brand_placement":["sport","premium"],"sustainability_placement":"ocean plastic",
				  "product_assumptions":["sneaker","mat"],"collaboration_summary":"A recycled-plastic sneaker.",
				  "estimated_impact":"75M","confidence_score":82,"combined_reach":"50M"}]}
				""";
	}
}
