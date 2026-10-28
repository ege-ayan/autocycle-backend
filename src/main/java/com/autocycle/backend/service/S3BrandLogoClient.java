package com.autocycle.backend.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.autocycle.backend.config.AppProperties;

@Service
public class S3BrandLogoClient implements LogoClient {

	private static final Logger log = LoggerFactory.getLogger(S3BrandLogoClient.class);

	private final BedrockGrounding grounding;
	private final BrandIconFetcher icons;
	private final ObjectStorage storage;
	private final AppProperties properties;

	public S3BrandLogoClient(
			BedrockGrounding grounding,
			BrandIconFetcher icons,
			ObjectStorage storage,
			AppProperties properties) {
		this.grounding = grounding;
		this.icons = icons;
		this.storage = storage;
		this.properties = properties;
	}

	@Override
	public Optional<Logo> find(String brandName) {
		Optional<String> domain = grounding.officialDomain(brandName);
		if (domain.isEmpty()) {
			return Optional.empty();
		}
		String logoUrl = null;
		if (StringUtils.hasText(properties.getS3Bucket())) {
			Optional<BrandIconFetcher.BrandIcon> icon = icons.fetch(domain.get());
			if (icon.isPresent()) {
				try {
					logoUrl = storage.upload(
							icon.get().bytes(),
							"brand-logos",
							domain.get(),
							icon.get().contentType(),
							icon.get().extension());
				} catch (RuntimeException exception) {
					log.warn("Brand logo upload failed for {}: {}", domain.get(), exception.getMessage());
				}
			}
		}
		return Optional.of(new Logo(logoUrl, domain.get()));
	}
}
