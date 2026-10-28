package com.autocycle.backend.service.model;

public record StoredProduct(
		String productName,
		String productType,
		String productDescription,
		String pitch,
		String imageUrl) {
}
