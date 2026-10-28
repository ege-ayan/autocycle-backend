package com.autocycle.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import com.autocycle.backend.entity.Brand;
import com.autocycle.backend.entity.PlasticType;
import com.autocycle.backend.support.PostgresTestcontainers;
import com.autocycle.backend.util.NameUuid;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainers.class)
@TestPropertySource(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false"
})
class BrandRepositoryTest {

	@Autowired
	private BrandRepository brands;

	@Autowired
	private PlasticTypeRepository plastics;

	@Test
	void storesBrandAndPlasticProfilesAsJson() {
		Brand brand = new Brand();
		brand.setBrandId(NameUuid.brandId("BMW"));
		brand.setName("BMW");
		brand.setIndustry("automotive");
		brand.setProductCategories(new ArrayList<>(List.of("cars")));
		brand.setPlasticMaterials(new ArrayList<>(List.of("PP")));
		brand.setSustainabilityPhilosophy("circular");
		brand.setKeyPartnersOrCollaborators(new ArrayList<>(List.of("UNEP")));
		brand.setManufacturingRegions(new ArrayList<>(List.of("Europe")));
		brand.setBrandPositioning("premium");
		brands.saveAndFlush(brand);

		PlasticType plastic = new PlasticType();
		plastic.setPlasticId(NameUuid.plasticId("PET"));
		plastic.setType("PET");
		plastic.setProperties(new ArrayList<>(List.of("clear")));
		plastic.setApplications(new ArrayList<>(List.of("bottles")));
		plastic.setEnvironmentalImpact("persistent");
		plastic.setRecyclingPotential("high");
		plastic.setRegionalRelevance("Europe");
		plastics.saveAndFlush(plastic);

		Brand stored = brands.findByBrandId(NameUuid.brandId("bmw")).orElseThrow();
		assertThat(stored.getProductCategories()).containsExactly("cars");
		assertThat(stored.getManufacturingRegions()).containsExactly("Europe");
		assertThat(plastics.findByPlasticId(NameUuid.plasticId("pet")).orElseThrow().getApplications()).containsExactly("bottles");
	}
}
