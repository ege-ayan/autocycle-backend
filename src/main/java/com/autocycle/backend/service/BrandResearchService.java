package com.autocycle.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.autocycle.backend.dto.ai.BrandProfileDraft;
import com.autocycle.backend.dto.ai.CollaborationDraft;
import com.autocycle.backend.dto.ai.PlasticProfileDraft;
import com.autocycle.backend.dto.requests.GenerateBrandRequest;
import com.autocycle.backend.dto.responses.CollaborationResponse;
import com.autocycle.backend.dto.responses.CollaborationResponse.Partner;
import com.autocycle.backend.exception.GenerationException;
import com.autocycle.backend.exception.InvalidRequestException;
import com.autocycle.backend.service.model.BrandProfile;
import com.autocycle.backend.service.model.PlasticProfile;
import com.autocycle.backend.util.JsonDocuments;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BrandResearchService {

	private final ProfileStore profiles;
	private final AiClient aiClient;
	private final WebSearchClient webSearch;
	private final LogoClient logos;
	private final JsonDocuments json;
	private final Executor executor;

	public BrandResearchService(
			ProfileStore profiles,
			AiClient aiClient,
			WebSearchClient webSearch,
			LogoClient logos,
			JsonDocuments json,
			@Qualifier("researchExecutor") Executor executor) {
		this.profiles = profiles;
		this.aiClient = aiClient;
		this.webSearch = webSearch;
		this.logos = logos;
		this.json = json;
		this.executor = executor;
	}

	public CollaborationResponse generate(GenerateBrandRequest request) {
		String brand = text(request.brand());
		String plastic = text(request.plasticType());
		if (brand.isBlank() && plastic.isBlank()) {
			throw new InvalidRequestException("Either brand or plastic type must be provided.");
		}
		try {
			CompletableFuture<Optional<BrandProfile>> brandFuture = CompletableFuture.supplyAsync(
					() -> resolveBrand(brand, text(request.location())), executor);
			CompletableFuture<Optional<PlasticProfile>> plasticFuture = CompletableFuture.supplyAsync(
					() -> resolvePlastic(plastic, text(request.location())), executor);
			CompletableFuture.allOf(brandFuture, plasticFuture).join();
			BrandProfile brandProfile = brandFuture.join().orElse(null);
			PlasticProfile plasticProfile = plasticFuture.join().orElse(null);
			CollaborationDraft draft = json.read(aiClient.complete(
					Prompts.COLLABORATION_SYSTEM,
					Prompts.collaborationUser(
							json.write(brandProfile),
							json.write(plasticProfile),
							text(request.location()),
							request.needPlastic()),
					true), CollaborationDraft.class);
			return new CollaborationResponse(brand, plastic, text(request.location()), request.needPlastic(), enrich(draft));
		} catch (CompletionException exception) {
			throw unwrap(exception);
		}
	}

	private Optional<BrandProfile> resolveBrand(String brand, String location) {
		if (brand.isBlank()) {
			return Optional.empty();
		}
		Optional<BrandProfile> known = profiles.findKnownBrand(brand);
		if (known.isPresent()) {
			log.info("Reusing stored brand profile for {}", brand);
			return known;
		}
		String evidence = webSearch.search(brand + " sustainability philosophy plastic materials manufacturing regions " + location);
		BrandProfileDraft draft = json.read(
				aiClient.complete(Prompts.BRAND_SYSTEM, Prompts.brandUser(brand, location, evidence), true),
				BrandProfileDraft.class);
		BrandProfile profile = BrandProfile.fromDraft(brand, draft);
		profiles.rememberBrand(profile);
		return Optional.of(profile);
	}

	private Optional<PlasticProfile> resolvePlastic(String plastic, String location) {
		if (plastic.isBlank()) {
			return Optional.empty();
		}
		Optional<PlasticProfile> known = profiles.findKnownPlastic(plastic);
		if (known.isPresent()) {
			log.info("Reusing stored plastic profile for {}", plastic);
			return known;
		}
		String evidence = webSearch.search(plastic + " polymer structure usage recycling sustainability " + location);
		PlasticProfileDraft draft = json.read(
				aiClient.complete(Prompts.PLASTIC_SYSTEM, Prompts.plasticUser(plastic, location, evidence), true),
				PlasticProfileDraft.class);
		PlasticProfile profile = PlasticProfile.fromDraft(plastic, draft);
		profiles.rememberPlastic(profile);
		return Optional.of(profile);
	}

	private List<Partner> enrich(CollaborationDraft draft) {
		List<CollaborationDraft.Recommendation> source = draft.topCollaborations() == null
				? List.of()
				: draft.topCollaborations();
		List<Partner> brands = new ArrayList<>();
		for (CollaborationDraft.Recommendation recommendation : source.stream().limit(5).toList()) {
			if (recommendation.brandName() == null || recommendation.brandName().isBlank()) {
				continue;
			}
			String logoUrl = null;
			String domain = null;
			Optional<LogoClient.Logo> logo = logos.find(recommendation.brandName());
			if (logo.isPresent()) {
				logoUrl = logo.get().logoUrl();
				domain = logo.get().companyDomain();
			}
			brands.add(new Partner(
					recommendation.brandName(),
					list(recommendation.brandPlacement()),
					recommendation.sustainabilityPlacement(),
					list(recommendation.productAssumptions()),
					recommendation.collaborationSummary(),
					recommendation.estimatedImpact(),
					recommendation.confidenceScore(),
					recommendation.combinedReach(),
					logoUrl,
					domain));
		}
		return brands;
	}

	private static List<String> list(List<String> values) {
		return values == null ? List.of() : values;
	}

	private static String text(String value) {
		return value == null ? "" : value.trim();
	}

	private static GenerationException unwrap(CompletionException exception) {
		Throwable cause = exception.getCause() == null ? exception : exception.getCause();
		if (cause instanceof GenerationException generationException) {
			return generationException;
		}
		return new GenerationException(cause.getMessage() == null ? "Brand research failed." : cause.getMessage(), cause);
	}
}
