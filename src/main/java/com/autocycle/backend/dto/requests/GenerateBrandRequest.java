package com.autocycle.backend.dto.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerateBrandRequest(
		String brand,
		String plasticType,
		String location,
		boolean needPlastic) {
}
