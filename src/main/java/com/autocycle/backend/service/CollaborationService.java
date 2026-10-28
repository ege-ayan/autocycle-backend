package com.autocycle.backend.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autocycle.backend.entity.Collaboration;
import com.autocycle.backend.entity.CollaborationProduct;
import com.autocycle.backend.repository.CollaborationProductRepository;
import com.autocycle.backend.repository.CollaborationRepository;
import com.autocycle.backend.service.model.StoredCollaboration;
import com.autocycle.backend.service.model.StoredProduct;

@Service
public class CollaborationService {

	private final CollaborationRepository collaborations;
	private final CollaborationProductRepository products;

	public CollaborationService(CollaborationRepository collaborations, CollaborationProductRepository products) {
		this.collaborations = collaborations;
		this.products = products;
	}

	@Transactional(readOnly = true)
	public Optional<StoredCollaboration> find(String sourceBrand, String targetBrand, String plasticType, String location) {
		return collaborations
				.findBySourceBrandIgnoreCaseAndTargetBrandIgnoreCaseAndPlasticTypeIgnoreCaseAndLocationIgnoreCase(
						sourceBrand, targetBrand, plasticType, location)
				.map(collaboration -> new StoredCollaboration(
						collaboration.getId(),
						collaboration.getSourceBrand(),
						collaboration.getTargetBrand(),
						collaboration.getPlasticType(),
						collaboration.getLocation(),
						collaboration.getProducts().stream()
								.map(product -> new StoredProduct(
										product.getProductName(),
										product.getProductType(),
										product.getProductDescription(),
										product.getPitch(),
										product.getImageUrl()))
								.toList()));
	}

	@Transactional
	public Long createOrGet(String sourceBrand, String targetBrand, String plasticType, String location) {
		String source = lower(sourceBrand);
		String target = lower(targetBrand);
		String plastic = lower(plasticType);
		String place = lower(location);
		return collaborations
				.findBySourceBrandIgnoreCaseAndTargetBrandIgnoreCaseAndPlasticTypeIgnoreCaseAndLocationIgnoreCase(
						source, target, plastic, place)
				.map(Collaboration::getId)
				.orElseGet(() -> {
					Collaboration collaboration = new Collaboration();
					collaboration.setSourceBrand(source);
					collaboration.setTargetBrand(target);
					collaboration.setPlasticType(plastic);
					collaboration.setLocation(place);
					return collaborations.save(collaboration).getId();
				});
	}

	@Transactional
	public void addProduct(Long collaborationId, StoredProduct product, Map<String, Object> fullResult) {
		Collaboration collaboration = collaborations.findById(collaborationId)
				.orElseThrow(() -> new IllegalStateException("Collaboration " + collaborationId + " was not found."));
		CollaborationProduct row = new CollaborationProduct();
		row.setCollaboration(collaboration);
		row.setProductName(product.productName());
		row.setProductType(product.productType());
		row.setProductDescription(product.productDescription());
		row.setPitch(product.pitch());
		row.setImageUrl(product.imageUrl());
		row.setFullResult(fullResult == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fullResult));
		products.save(row);
	}

	private static String lower(String value) {
		return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
	}
}
