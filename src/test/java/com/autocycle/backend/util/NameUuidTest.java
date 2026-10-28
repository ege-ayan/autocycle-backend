package com.autocycle.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameUuidTest {

	@Test
	void matchesPythonUuid5ForBrandsAndPlastics() {
		assertThat(NameUuid.brandId("BMW")).isEqualTo("3208ed61-6a18-539c-9e35-ed27cfe1d9d7");
		assertThat(NameUuid.plasticId("PET")).isEqualTo("478b2135-bf31-5767-a1eb-939a60fb5709");
	}

	@Test
	void ignoresCaseAndSurroundingSpace() {
		assertThat(NameUuid.brandId("  bmw ")).isEqualTo(NameUuid.brandId("BMW"));
	}
}
