package com.autocycle.backend.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SimilarityTest {

	@Test
	void convertsWeaviateCosineDistanceIntoThePythonScore() {
		assertThat(Similarity.fromCosineDistance(0)).isEqualTo(1.0);
		assertThat(Similarity.fromCosineDistance(0.4)).isEqualTo(0.8);
		assertThat(Similarity.fromCosineDistance(2)).isEqualTo(0.0);
	}

	@Test
	void treatsTheThresholdAsACacheHit() {
		assertThat(Similarity.matches(0.8, 0.8)).isTrue();
		assertThat(Similarity.matches(0.79, 0.8)).isFalse();
	}
}
