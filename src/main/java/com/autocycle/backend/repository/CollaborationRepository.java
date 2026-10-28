package com.autocycle.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.autocycle.backend.entity.Collaboration;

public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {

	@EntityGraph(attributePaths = "products")
	Optional<Collaboration> findBySourceBrandIgnoreCaseAndTargetBrandIgnoreCaseAndPlasticTypeIgnoreCaseAndLocationIgnoreCase(
			String sourceBrand,
			String targetBrand,
			String plasticType,
			String location);
}
