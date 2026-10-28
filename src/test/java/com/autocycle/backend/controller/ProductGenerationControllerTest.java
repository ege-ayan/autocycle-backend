package com.autocycle.backend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
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

import com.autocycle.backend.dto.requests.GenerateProductsRequest;
import com.autocycle.backend.dto.responses.ProductResponse;
import com.autocycle.backend.dto.responses.ProductResponse.Product;
import com.autocycle.backend.exception.GlobalExceptionHandler;
import com.autocycle.backend.service.ProductGenerationService;

@WebMvcTest(ProductGenerationController.class)
@Import(GlobalExceptionHandler.class)
class ProductGenerationControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ProductGenerationService productGenerationService;

	@Test
	void returnsTheGeneratedProduct() throws Exception {
		when(productGenerationService.generate(any(GenerateProductsRequest.class))).thenReturn(
				new ProductResponse(false, "BMW", "Adidas", "PET", "Munich", List.of(
						new Product("Ocean Runner", "sneaker", "A shoe.", "pitch", "https://cdn.test/shoe.png"))));

		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"sourceBrand":"BMW","plasticType":"PET","location":"Munich","targetBrand":"Adidas"}
								"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.cached").value(false))
				.andExpect(jsonPath("$.products[0].name").value("Ocean Runner"))
				.andExpect(jsonPath("$.targetBrand").value("Adidas"))
				.andExpect(jsonPath("$.products[0].imageUrl").value("https://cdn.test/shoe.png"));

		ArgumentCaptor<GenerateProductsRequest> captor = ArgumentCaptor.forClass(GenerateProductsRequest.class);
		verify(productGenerationService).generate(captor.capture());
		org.assertj.core.api.Assertions.assertThat(captor.getValue().sourceBrand()).isEqualTo("BMW");
		org.assertj.core.api.Assertions.assertThat(captor.getValue().targetBrand()).isEqualTo("Adidas");
		org.assertj.core.api.Assertions.assertThat(captor.getValue().plasticType()).isEqualTo("PET");
	}

	@Test
	void rejectsARequestMissingTheBrands() throws Exception {
		mockMvc.perform(post("/api/products")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"plasticType":"PET","location":"Munich"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.fieldErrors.sourceBrand").exists())
				.andExpect(jsonPath("$.fieldErrors.targetBrand").exists());
	}
}
