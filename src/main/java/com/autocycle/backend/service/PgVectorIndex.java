package com.autocycle.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.autocycle.backend.config.AppProperties;
import com.autocycle.backend.service.model.VectorHit;
import com.autocycle.backend.util.EmbeddingText;
import com.autocycle.backend.util.Similarity;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PgVectorIndex {

	public static final int DIMENSIONS = 1024;

	private final JdbcTemplate jdbcTemplate;
	private final AiClient aiClient;
	private final AppProperties properties;

	public PgVectorIndex(JdbcTemplate jdbcTemplate, AiClient aiClient, AppProperties properties) {
		this.jdbcTemplate = jdbcTemplate;
		this.aiClient = aiClient;
		this.properties = properties;
	}

	public boolean enabled() {
		return properties.isVectorEnabled();
	}

	public Optional<VectorHit> nearestBrand(String name) {
		if (!enabled() || name == null || name.isBlank()) {
			return Optional.empty();
		}
		return nearest("brand_embedding", "brand_id", EmbeddingText.brandQuery(name));
	}

	public Optional<VectorHit> nearestPlastic(String type) {
		if (!enabled() || type == null || type.isBlank()) {
			return Optional.empty();
		}
		return nearest("plastic_embedding", "plastic_id", EmbeddingText.plasticQuery(type));
	}

	public void upsertBrand(String brandId, String text) {
		upsert("brand_embedding", "brand_id", brandId, text);
	}

	public void upsertPlastic(String plasticId, String text) {
		upsert("plastic_embedding", "plastic_id", plasticId, text);
	}

	private Optional<VectorHit> nearest(String table, String idColumn, String queryText) {
		try {
			List<Double> vector = requireVector(aiClient.embed(queryText));
			String literal = literal(vector);
			List<VectorHit> hits = jdbcTemplate.query(
					"""
					SELECT %s AS entity_id, (embedding <=> CAST(? AS vector)) AS distance
					FROM %s
					WHERE provider = ? AND model = ?
					ORDER BY embedding <=> CAST(? AS vector)
					LIMIT 1
					""".formatted(idColumn, table),
					(resultSet, row) -> new VectorHit(
							resultSet.getString("entity_id"),
							Similarity.fromCosineDistance(resultSet.getDouble("distance"))),
					literal,
					aiClient.provider(),
					aiClient.embeddingModel(),
					literal);
			return hits.stream().findFirst();
		} catch (RuntimeException exception) {
			log.warn("Vector search skipped: {}", exception.getMessage());
			return Optional.empty();
		}
	}

	private void upsert(String table, String idColumn, String id, String text) {
		if (!enabled()) {
			return;
		}
		try {
			List<Double> vector = requireVector(aiClient.embed(text));
			jdbcTemplate.update(
					"""
					INSERT INTO %s (%s, provider, model, embedding)
					VALUES (?, ?, ?, CAST(? AS vector))
					ON CONFLICT (%s) DO UPDATE SET
					  provider = EXCLUDED.provider,
					  model = EXCLUDED.model,
					  embedding = EXCLUDED.embedding,
					  updated_at = CURRENT_TIMESTAMP
					""".formatted(table, idColumn, idColumn),
					id,
					aiClient.provider(),
					aiClient.embeddingModel(),
					literal(vector));
		} catch (RuntimeException exception) {
			log.warn("Embedding was not stored for {}: {}", id, exception.getMessage());
		}
	}

	private static List<Double> requireVector(List<Double> vector) {
		if (vector == null || vector.size() != DIMENSIONS) {
			throw new IllegalStateException("Expected a " + DIMENSIONS + "-dimension embedding.");
		}
		return vector;
	}

	private static String literal(List<Double> vector) {
		return vector.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
	}
}
