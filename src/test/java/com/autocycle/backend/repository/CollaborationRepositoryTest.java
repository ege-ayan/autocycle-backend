package com.autocycle.backend.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import jakarta.persistence.EntityManager;

import com.autocycle.backend.entity.Collaboration;
import com.autocycle.backend.entity.CollaborationProduct;
import com.autocycle.backend.support.PostgresTestcontainers;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(PostgresTestcontainers.class)
@TestPropertySource(properties = {
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false"
})
class CollaborationRepositoryTest {

	@Autowired
	private CollaborationRepository collaborations;

	@Autowired
	private CollaborationProductRepository products;

	@Autowired
	private EntityManager entityManager;

	@Test
	void findsACollaborationAndItsProductsIgnoringCase() {
		Collaboration collaboration = new Collaboration();
		collaboration.setSourceBrand("bmw");
		collaboration.setTargetBrand("adidas");
		collaboration.setPlasticType("pet");
		collaboration.setLocation("munich");
		collaborations.save(collaboration);

		CollaborationProduct product = new CollaborationProduct();
		product.setCollaboration(collaboration);
		product.setProductName("Ocean Runner");
		product.setProductType("sneaker");
		product.setProductDescription("A recycled shoe.");
		product.setPitch("pitch");
		product.setImageUrl("https://cdn.test/shoe.png");
		product.setFullResult(Map.of("product_name", "Ocean Runner"));
		products.saveAndFlush(product);
		entityManager.clear();

		var found = collaborations
				.findBySourceBrandIgnoreCaseAndTargetBrandIgnoreCaseAndPlasticTypeIgnoreCaseAndLocationIgnoreCase(
						"BMW", "Adidas", "PET", "Munich");

		assertThat(found).isPresent();
		assertThat(found.get().getProducts()).hasSize(1);
		assertThat(found.get().getProducts().getFirst().getProductName()).isEqualTo("Ocean Runner");
		assertThat(found.get().getProducts().getFirst().getFullResult()).containsEntry("product_name", "Ocean Runner");
	}
}
