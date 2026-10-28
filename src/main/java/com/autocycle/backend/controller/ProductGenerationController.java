package com.autocycle.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autocycle.backend.dto.requests.GenerateProductsRequest;
import com.autocycle.backend.dto.responses.ProductResponse;
import com.autocycle.backend.service.ProductGenerationService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/products")
public class ProductGenerationController {

	private final ProductGenerationService productGenerationService;

	public ProductGenerationController(ProductGenerationService productGenerationService) {
		this.productGenerationService = productGenerationService;
	}

	@PostMapping
	public ProductResponse generate(@Valid @RequestBody GenerateProductsRequest request) {
		return productGenerationService.generate(request);
	}
}
