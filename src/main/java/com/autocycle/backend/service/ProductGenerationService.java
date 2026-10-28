package com.autocycle.backend.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.autocycle.backend.dto.ai.BrandProfileDraft;
import com.autocycle.backend.dto.ai.PlasticProfileDraft;
import com.autocycle.backend.dto.ai.ProductIdeasDraft;
import com.autocycle.backend.dto.requests.GenerateProductsRequest;
import com.autocycle.backend.dto.responses.ProductResponse;
import com.autocycle.backend.dto.responses.ProductResponse.Product;
import com.autocycle.backend.exception.GenerationException;
import com.autocycle.backend.exception.InvalidRequestException;
import com.autocycle.backend.service.model.BrandProfile;
import com.autocycle.backend.service.model.PlasticProfile;
import com.autocycle.backend.service.model.StoredCollaboration;
import com.autocycle.backend.service.model.StoredProduct;
import com.autocycle.backend.util.JsonDocuments;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductGenerationService {

	private final CollaborationService collaborations;
	private final ProfileStore profiles;
	private final AiClient aiClient;
	private final WebSearchClient webSearch;
	private final ProductImageService images;
	private final JsonDocuments json;
	private final Executor executor;

	public ProductGenerationService(
			CollaborationService collaborations,
			ProfileStore profiles,
			AiClient aiClient,
			WebSearchClient webSearch,
			ProductImageService images,
			JsonDocuments json,
			@Qualifier("researchExecutor") Executor executor) {
		this.collaborations = collaborations;
		this.profiles = profiles;
		this.aiClient = aiClient;
		this.webSearch = webSearch;
		this.images = images;
		this.json = json;
		this.executor = executor;
	}

	public ProductResponse generate(GenerateProductsRequest request) {
		String source = text(request.sourceBrand());
		String target = text(request.targetBrand());
		String plastic = text(request.plasticType());
		String location = text(request.location());
		if (source.isBlank() || target.isBlank() || plastic.isBlank()) {
			throw new InvalidRequestException("Source brand, target brand, and plastic type are required.");
		}
		Optional<StoredCollaboration> existing = collaborations.find(source, target, plastic, location);
		if (existing.isPresent() && !existing.get().products().isEmpty()) {
			return cached(source, target, plastic, location, existing.get());
		}
		try {
			CompletableFuture<BrandProfile> sourceFuture = CompletableFuture.supplyAsync(
					() -> resolveBrand(source, location), executor);
			CompletableFuture<BrandProfile> targetFuture = CompletableFuture.supplyAsync(
					() -> resolveBrand(target, location), executor);
			CompletableFuture<PlasticProfile> plasticFuture = CompletableFuture.supplyAsync(
					() -> resolvePlastic(plastic, location), executor);
			CompletableFuture.allOf(sourceFuture, targetFuture, plasticFuture).join();
			ProductIdeasDraft ideas = json.read(aiClient.complete(
					Prompts.PRODUCT_IDEA_SYSTEM,
					Prompts.productIdeaUser(
							json.write(sourceFuture.join()),
							json.write(targetFuture.join()),
							json.write(plasticFuture.join())),
					true), ProductIdeasDraft.class);
			if (ideas.products() == null || ideas.products().isEmpty()) {
				throw new GenerationException("The model did not return a product concept.");
			}
			Long collaborationId = collaborations.createOrGet(source, target, plastic, location);
			List<Product> products = new ArrayList<>();
			for (ProductIdeasDraft.ProductIdea idea : ideas.products()) {
				products.add(develop(collaborationId, source, target, idea));
			}
			return new ProductResponse(false, source, target, plastic, location, products);
		} catch (CompletionException exception) {
			Throwable cause = exception.getCause() == null ? exception : exception.getCause();
			if (cause instanceof GenerationException generationException) {
				throw generationException;
			}
			throw new GenerationException(cause.getMessage() == null ? "Product generation failed." : cause.getMessage(), cause);
		}
	}

	private ProductResponse cached(
			String sourceBrand,
			String targetBrand,
			String plasticType,
			String location,
			StoredCollaboration collaboration) {
		List<Product> products = collaboration.products().stream()
				.map(product -> new Product(
						product.productName(),
						product.productType(),
						product.productDescription(),
						product.pitch(),
						product.imageUrl()))
				.toList();
		return new ProductResponse(true, sourceBrand, targetBrand, plasticType, location, products);
	}

	private Product develop(Long collaborationId, String source, String target, ProductIdeasDraft.ProductIdea idea) {
		String name = text(idea.name());
		if (name.isBlank()) {
			throw new GenerationException("The model returned a product without a name.");
		}
		String pitch = aiClient.complete(
				Prompts.PITCH_SYSTEM,
				Prompts.pitchUser(source, target, idea.productType(), name, idea.description()),
				false);
		String imageUrl = null;
		try {
			imageUrl = images.create(Prompts.imagePrompt(source, target, idea.productType(), name), name);
		} catch (RuntimeException exception) {
			log.warn("Image generation failed for {}: {}", name, exception.getMessage());
		}
		Product product = new Product(name, idea.productType(), idea.description(), pitch, imageUrl);
		collaborations.addProduct(collaborationId, new StoredProduct(name, idea.productType(), idea.description(), pitch, imageUrl), fullResult(product));
		return product;
	}

	private BrandProfile resolveBrand(String brand, String location) {
		return profiles.findKnownBrand(brand).orElseGet(() -> {
			String evidence = webSearch.search(brand + " sustainability philosophy plastic materials manufacturing regions " + location);
			BrandProfileDraft draft = json.read(
					aiClient.complete(Prompts.BRAND_SYSTEM, Prompts.brandUser(brand, location, evidence), true),
					BrandProfileDraft.class);
			BrandProfile profile = BrandProfile.fromDraft(brand, draft);
			profiles.rememberBrand(profile);
			return profile;
		});
	}

	private PlasticProfile resolvePlastic(String plastic, String location) {
		return profiles.findKnownPlastic(plastic).orElseGet(() -> {
			String evidence = webSearch.search(plastic + " polymer structure usage recycling sustainability " + location);
			PlasticProfileDraft draft = json.read(
					aiClient.complete(Prompts.PLASTIC_SYSTEM, Prompts.plasticUser(plastic, location, evidence), true),
					PlasticProfileDraft.class);
			PlasticProfile profile = PlasticProfile.fromDraft(plastic, draft);
			profiles.rememberPlastic(profile);
			return profile;
		});
	}

	private Map<String, Object> fullResult(Product product) {
		Map<String, Object> payload = new LinkedHashMap<>();
		payload.put("name", product.name());
		payload.put("type", product.type());
		payload.put("description", product.description());
		payload.put("pitch", product.pitch());
		payload.put("imageUrl", product.imageUrl());
		return payload;
	}

	private static String text(String value) {
		return value == null ? "" : value.trim();
	}
}
