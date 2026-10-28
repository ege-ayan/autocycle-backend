package com.autocycle.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autocycle.backend.entity.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {

	Optional<Brand> findByBrandId(String brandId);

	boolean existsByBrandId(String brandId);
}
