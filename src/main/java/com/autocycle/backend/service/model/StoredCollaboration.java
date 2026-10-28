package com.autocycle.backend.service.model;

import java.util.List;

public record StoredCollaboration(
		Long id,
		String sourceBrand,
		String targetBrand,
		String plasticType,
		String location,
		List<StoredProduct> products) {
}
