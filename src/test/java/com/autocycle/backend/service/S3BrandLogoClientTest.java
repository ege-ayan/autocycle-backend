package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.autocycle.backend.config.AppProperties;

class S3BrandLogoClientTest {

	private final BedrockGrounding grounding = mock(BedrockGrounding.class);
	private final BrandIconFetcher icons = mock(BrandIconFetcher.class);
	private final ObjectStorage storage = mock(ObjectStorage.class);
	private final AppProperties properties = new AppProperties();
	private final S3BrandLogoClient client = new S3BrandLogoClient(grounding, icons, storage, properties);

	@Test
	void storesTheSiteIconAndKeepsTheDomain() {
		properties.setS3Bucket("autocycle-product-images");
		byte[] png = {1, 2, 3};
		when(grounding.officialDomain("Adidas")).thenReturn(Optional.of("adidas.com"));
		when(icons.fetch("adidas.com")).thenReturn(Optional.of(
				new BrandIconFetcher.BrandIcon(png, "image/png", "png")));
		when(storage.upload(png, "brand-logos", "adidas.com", "image/png", "png"))
				.thenReturn("https://cdn.test/brand-logos/adidas.com.png");

		LogoClient.Logo logo = client.find("Adidas").orElseThrow();

		assertThat(logo.logoUrl()).isEqualTo("https://cdn.test/brand-logos/adidas.com.png");
		assertThat(logo.companyDomain()).isEqualTo("adidas.com");
	}

	@Test
	void skipsTheLookupWhenNoDomainIsFound() {
		when(grounding.officialDomain("Unknown")).thenReturn(Optional.empty());

		assertThat(client.find("Unknown")).isEmpty();
		verifyNoInteractions(icons, storage);
	}
}
