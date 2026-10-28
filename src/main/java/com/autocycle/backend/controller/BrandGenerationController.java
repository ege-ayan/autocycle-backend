package com.autocycle.backend.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.autocycle.backend.dto.requests.GenerateBrandRequest;
import com.autocycle.backend.dto.responses.CollaborationResponse;
import com.autocycle.backend.service.BrandResearchService;

@RestController
@RequestMapping("/api/collaborations")
public class BrandGenerationController {

	private final BrandResearchService brandResearchService;

	public BrandGenerationController(BrandResearchService brandResearchService) {
		this.brandResearchService = brandResearchService;
	}

	@PostMapping
	public CollaborationResponse generate(@RequestBody GenerateBrandRequest request) {
		return brandResearchService.generate(request);
	}
}
