package com.autocycle.backend.util;

public final class Similarity {

	private Similarity() {
	}

	/**
	 * Weaviate cosine distance is in [0, 2]. The Python flow turned that into a
	 * score with {@code 1 - distance / 2} and treated 0.8 as a cache hit.
	 */
	public static double fromCosineDistance(double distance) {
		double score = 1.0 - (distance / 2.0);
		return Math.max(0.0, Math.min(1.0, score));
	}

	public static boolean matches(double score, double threshold) {
		return Double.compare(score, threshold) >= 0;
	}
}
