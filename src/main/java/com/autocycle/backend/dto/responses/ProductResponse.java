package com.autocycle.backend.dto.responses;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(
		boolean cached,
		String sourceBrand,
		String targetBrand,
		String plasticType,
		String location,
		List<Product> products) {

	public record Product(
			String name,
			String type,
			String description,
			String pitch,
			String imageUrl) {
	}
}
