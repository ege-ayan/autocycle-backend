package com.autocycle.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.autocycle.backend.entity.Brand;
import com.autocycle.backend.repository.BrandRepository;
import com.autocycle.backend.service.BedrockAiClient;
import com.autocycle.backend.service.PgVectorIndex;
import com.autocycle.backend.service.model.VectorHit;
import com.autocycle.backend.support.PostgresTestcontainers;
import com.autocycle.backend.util.NameUuid;

@SpringBootTest
@Import(PostgresTestcontainers.class)
class VectorIndexIntegrationTest {

	@Autowired
	private PgVectorIndex vectorIndex;

	@Autowired
	private BrandRepository brands;

	@MockitoBean
	private BedrockAiClient aiClient;

	@Test
	void ranksAnIdenticalEmbeddingAsAPerfectMatch() {
		when(aiClient.provider()).thenReturn("bedrock");
		when(aiClient.embeddingModel()).thenReturn("amazon.titan-embed-text-v2:0");
		when(aiClient.embed(anyString())).thenReturn(vector());

		Brand brand = new Brand();
		brand.setBrandId(NameUuid.brandId("BMW"));
		brand.setName("BMW");
		brand.setProductCategories(new ArrayList<>());
		brand.setPlasticMaterials(new ArrayList<>());
		brand.setKeyPartnersOrCollaborators(new ArrayList<>());
		brand.setManufacturingRegions(new ArrayList<>());
		brands.saveAndFlush(brand);

		vectorIndex.upsertBrand(brand.getBrandId(), "bmw is a brand");
		VectorHit hit = vectorIndex.nearestBrand("BMW").orElseThrow();

		assertThat(hit.id()).isEqualTo(brand.getBrandId());
		assertThat(hit.similarity()).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.001));
	}

	private static List<Double> vector() {
		List<Double> values = new ArrayList<>();
		values.add(1.0);
		for (int index = 1; index < PgVectorIndex.DIMENSIONS; index++) {
			values.add(0.0);
		}
		return values;
	}
}
