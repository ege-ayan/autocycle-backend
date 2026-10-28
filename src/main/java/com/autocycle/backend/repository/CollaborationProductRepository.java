package com.autocycle.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.autocycle.backend.entity.CollaborationProduct;

public interface CollaborationProductRepository extends JpaRepository<CollaborationProduct, Long> {
}
