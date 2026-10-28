package com.autocycle.backend.service;

import org.springframework.stereotype.Service;

@Service
public class BedrockWebSearchClient implements WebSearchClient {

	private final BedrockGrounding grounding;

	public BedrockWebSearchClient(BedrockGrounding grounding) {
		this.grounding = grounding;
	}

	@Override
	public String search(String query) {
		return grounding.evidence(query);
	}
}
