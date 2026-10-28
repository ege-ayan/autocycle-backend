package com.autocycle.backend.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.autocycle.backend.config.AppProperties;
import com.autocycle.backend.entity.Brand;
import com.autocycle.backend.entity.PlasticType;
import com.autocycle.backend.repository.BrandRepository;
import com.autocycle.backend.repository.PlasticTypeRepository;
import com.autocycle.backend.service.model.BrandProfile;
import com.autocycle.backend.service.model.PlasticProfile;
import com.autocycle.backend.service.model.VectorHit;
import com.autocycle.backend.util.EmbeddingText;
import com.autocycle.backend.util.NameUuid;
import com.autocycle.backend.util.Similarity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProfileStore {

	private final BrandRepository brands;
	private final PlasticTypeRepository plastics;
	private final PgVectorIndex vectorIndex;
	private final AppProperties properties;

	public ProfileStore(
			BrandRepository brands,
			PlasticTypeRepository plastics,
			PgVectorIndex vectorIndex,
			AppProperties properties) {
		this.brands = brands;
		this.plastics = plastics;
		this.vectorIndex = vectorIndex;
		this.properties = properties;
	}

	@Transactional(readOnly = true)
	public Optional<BrandProfile> findKnownBrand(String name) {
		if (name == null || name.isBlank()) {
			return Optional.empty();
		}
		Optional<Brand> exact = brands.findByBrandId(NameUuid.brandId(name));
		if (exact.isPresent()) {
			return Optional.of(BrandProfile.from(exact.get()).withName(name.trim()));
		}
		Optional<VectorHit> hit = vectorIndex.nearestBrand(name);
		if (hit.isEmpty() || !Similarity.matches(hit.get().similarity(), properties.getSimilarityThreshold())) {
			return Optional.empty();
		}
		return brands.findByBrandId(hit.get().id()).map(brand -> BrandProfile.from(brand).withName(name.trim()));
	}

	@Transactional(readOnly = true)
	public Optional<PlasticProfile> findKnownPlastic(String type) {
		if (type == null || type.isBlank()) {
			return Optional.empty();
		}
		Optional<PlasticType> exact = plastics.findByPlasticId(NameUuid.plasticId(type));
		if (exact.isPresent()) {
			return Optional.of(PlasticProfile.from(exact.get()).withType(type.trim()));
		}
		Optional<VectorHit> hit = vectorIndex.nearestPlastic(type);
		if (hit.isEmpty() || !Similarity.matches(hit.get().similarity(), properties.getSimilarityThreshold())) {
			return Optional.empty();
		}
		return plastics.findByPlasticId(hit.get().id()).map(plastic -> PlasticProfile.from(plastic).withType(type.trim()));
	}

	@Transactional
	public void rememberBrand(BrandProfile profile) {
		String brandId = NameUuid.brandId(profile.name());
		if (brands.existsByBrandId(brandId)) {
			log.info("Brand {} is already stored", brandId);
			return;
		}
		Brand brand = new Brand();
		brand.setBrandId(brandId);
		brand.setName(profile.name().trim());
		brand.setIndustry(profile.industry());
		brand.setProductCategories(profile.mutable(profile.productCategories()));
		brand.setPlasticMaterials(profile.mutable(profile.plasticMaterials()));
		brand.setSustainabilityPhilosophy(profile.sustainabilityPhilosophy());
		brand.setKeyPartnersOrCollaborators(profile.mutable(profile.keyPartnersOrCollaborators()));
		brand.setManufacturingRegions(profile.mutable(profile.manufacturingRegions()));
		brand.setBrandPositioning(profile.brandPositioning());
		brands.save(brand);
		vectorIndex.upsertBrand(brandId, EmbeddingText.brand(profile));
	}

	@Transactional
	public void rememberPlastic(PlasticProfile profile) {
		String plasticId = NameUuid.plasticId(profile.type());
		PlasticType plastic = plastics.findByPlasticId(plasticId).orElseGet(PlasticType::new);
		plastic.setPlasticId(plasticId);
		plastic.setType(profile.type().trim());
		plastic.setProperties(new java.util.ArrayList<>(profile.properties()));
		plastic.setApplications(new java.util.ArrayList<>(profile.applications()));
		plastic.setEnvironmentalImpact(profile.environmentalImpact());
		plastic.setRecyclingPotential(profile.recyclingPotential());
		plastic.setRegionalRelevance(profile.regionalRelevance());
		plastics.save(plastic);
		vectorIndex.upsertPlastic(plasticId, EmbeddingText.plastic(profile));
	}
}
