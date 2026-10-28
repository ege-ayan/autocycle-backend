package com.autocycle.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.autocycle.backend.dto.requests.GenerateBrandRequest;
import com.autocycle.backend.dto.responses.CollaborationResponse;
import com.autocycle.backend.dto.responses.CollaborationResponse.Partner;
import com.autocycle.backend.exception.GenerationException;
import com.autocycle.backend.exception.GlobalExceptionHandler;
import com.autocycle.backend.service.BrandResearchService;

@WebMvcTest(BrandGenerationController.class)
@Import(GlobalExceptionHandler.class)
class BrandGenerationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private BrandResearchService brandResearchService;

	@Test
	void returnsPartners() throws Exception {
		when(brandResearchService.generate(any(GenerateBrandRequest.class))).thenReturn(new CollaborationResponse(
				"BMW",
				"PET",
				"Munich",
				false,
				List.of(new Partner(
						"Adidas", List.of("sport"), "ocean plastic", List.of("sneaker"),
						"A recycled sneaker.", "75M", 82, "50M", "https://logo.test/a.png", "adidas.com"))));

		mockMvc.perform(post("/api/collaborations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand":"BMW","plasticType":"PET","location":"Munich","needPlastic":false}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.brand").value("BMW"))
				.andExpect(jsonPath("$.plasticType").value("PET"))
				.andExpect(jsonPath("$.partners[0].brandName").value("Adidas"))
				.andExpect(jsonPath("$.partners[0].confidenceScore").value(82))
				.andExpect(jsonPath("$.partners[0].logoUrl").value("https://logo.test/a.png"));

		ArgumentCaptor<GenerateBrandRequest> captor = ArgumentCaptor.forClass(GenerateBrandRequest.class);
		org.mockito.Mockito.verify(brandResearchService).generate(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().plasticType()).isEqualTo("PET");
		org.assertj.core.api.Assertions.assertThat(captor.getValue().needPlastic()).isFalse();
		org.assertj.core.api.Assertions.assertThat(captor.getValue().location()).isEqualTo("Munich");
	}

	@Test
	void reportsUpstreamFailures() throws Exception {
		when(brandResearchService.generate(any(GenerateBrandRequest.class)))
				.thenThrow(new GenerationException("Bedrock returned an empty response."));

		mockMvc.perform(post("/api/collaborations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand":"BMW","plasticType":"PET","location":"Munich","needPlastic":true}
								"""))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.status").value(502))
				.andExpect(jsonPath("$.message").value("Bedrock returned an empty response."));
	}

	@Test
	void reportsUnexpectedFailures() throws Exception {
		when(brandResearchService.generate(any(GenerateBrandRequest.class)))
				.thenThrow(new IllegalStateException("database disappeared"));

		mockMvc.perform(post("/api/collaborations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"brand":"BMW","plasticType":"PET","location":"Munich","needPlastic":false}
								"""))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.status").value(500))
				.andExpect(jsonPath("$.message").value("Unexpected server error"));
	}

	@Test
	void rejectsMalformedJson() throws Exception {
		mockMvc.perform(post("/api/collaborations")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.message").exists());
	}
}
