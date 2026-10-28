package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class BrandDomainsTest {

	@Test
	void prefersTheDomainWrittenInTheAnswer() {
		assertThat(BrandDomains.pick("Adidas", "adidas.com", List.of("wikipedia.org")))
				.contains("adidas.com");
	}

	@Test
	void fallsBackToACitationThatMatchesTheBrand() {
		assertThat(BrandDomains.pick("Coca-Cola", "I am not sure.", List.of("https://www.coca-cola.com/brands")))
				.contains("coca-cola.com");
	}

	@Test
	void ignoresUnrelatedCitations() {
		assertThat(BrandDomains.pick("Adidas", "", List.of("wikipedia.org"))).isEmpty();
	}
}
