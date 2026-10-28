package com.autocycle.backend.integration;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.autocycle.backend.service.BedrockAiClient;
import com.autocycle.backend.service.BedrockWebSearchClient;
import com.autocycle.backend.service.ImageService;
import com.autocycle.backend.service.S3BrandLogoClient;
import com.autocycle.backend.support.PostgresTestcontainers;

@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainers.class)
class GenerationIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BedrockAiClient aiClient;

	@MockitoBean
	private ImageService imageService;

	@MockitoBean
	private S3BrandLogoClient logoClient;

	@MockitoBean
	private BedrockWebSearchClient webSearchClient;

	@Test
	void generatesABrandMatchAndThenCachesTheProduct() throws Exception {
		when(webSearchClient.search(anyString())).thenReturn("");
		when(logoClient.find(anyString())).thenReturn(Optional.of(new com.autocycle.backend.service.LogoClient.Logo(
				"https://logo.test/adidas.png", "adidas.com")));
		when(aiClient.complete(anyString(), anyString(), anyBoolean())).thenAnswer(invocation -> {
			String system = invocation.getArgument(0);
			boolean json = invocation.getArgument(2);
			if (!json) {
				return "**Concept:** A recycled sneaker.";
			}
			if (system.contains("polymer expert")) {
				return """
						{"type":"PET","properties":["clear"],"applications":["bottles"],"environmental_impact":"persistent","recycling_potential":"high","regional_relevance":"Europe"}
						""";
			}
			if (system.contains("collaboration strategist")) {
				return """
						{"top_collaborations":[{"brand_name":"Adidas","brand_placement":["sport"],"sustainability_placement":"ocean plastic","product_assumptions":["sneaker"],"collaboration_summary":"A shoe.","estimated_impact":"75M","confidence_score":80,"combined_reach":"50M"}]}
						""";
			}
			if (system.contains("exactly one")) {
				return """
						{"products":[{"product_type":"sneaker","name":"Ocean Runner","description":"A recycled performance sneaker for city miles."}]}
						""";
			}
			return """
					{"industry":"automotive","product_categories":["cars"],"plastic_materials":["PP"],"sustainability_philosophy":"circular","key_partners_or_collaborators":[],"manufacturing_regions":["Europe"],"brand_positioning":"premium"}
					""";
		});
		when(imageService.create(anyString(), anyString())).thenReturn("https://cdn.test/ocean.png");

		mockMvc.perform(post("/api/collaborations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand":"BMW","plasticType":"PET","location":"Munich","needPlastic":false}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.partners[0].brandName").value("Adidas"))
				.andExpect(jsonPath("$.partners[0].companyDomain").value("adidas.com"));

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sourceBrand":"BMW","plasticType":"PET","location":"Munich","targetBrand":"Adidas"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cached").value(false))
				.andExpect(jsonPath("$.products[0].imageUrl").value("https://cdn.test/ocean.png"));

		clearInvocations(aiClient);
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sourceBrand":"BMW","plasticType":"PET","location":"Munich","targetBrand":"Adidas"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cached").value(true))
				.andExpect(jsonPath("$.products[0].name").value("Ocean Runner"));
		verify(aiClient, never()).complete(anyString(), anyString(), anyBoolean());
	}
}
