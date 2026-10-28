package com.autocycle.backend.dto.requests;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GenerateProductsRequest(
		@NotBlank String sourceBrand,
		@NotBlank String plasticType,
		String location,
		@NotBlank String targetBrand) {
}
