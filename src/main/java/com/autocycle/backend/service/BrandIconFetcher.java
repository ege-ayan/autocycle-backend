package com.autocycle.backend.service;

import java.util.Optional;

public interface BrandIconFetcher {

	Optional<BrandIcon> fetch(String domain);

	record BrandIcon(byte[] bytes, String contentType, String extension) {
	}
}
