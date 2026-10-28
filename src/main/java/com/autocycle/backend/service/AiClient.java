package com.autocycle.backend.service;

import java.util.List;

public interface AiClient {

	String complete(String systemPrompt, String userPrompt, boolean json);

	List<Double> embed(String text);

	String provider();

	String embeddingModel();
}
