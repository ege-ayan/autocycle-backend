package com.autocycle.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autocycle.backend.entity.PlasticType;

public interface PlasticTypeRepository extends JpaRepository<PlasticType, Long> {

	Optional<PlasticType> findByPlasticId(String plasticId);

	boolean existsByPlasticId(String plasticId);
}
