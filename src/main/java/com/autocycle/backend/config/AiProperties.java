package com.autocycle.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

	private Bedrock bedrock = new Bedrock();

	@Getter
	@Setter
	public static class Bedrock {
		private String model = "eu.amazon.nova-pro-v1:0";
		private String embeddingModel = "amazon.titan-embed-text-v2:0";
		private String imageModel = "amazon.nova-canvas-v1:0";
	}
}
