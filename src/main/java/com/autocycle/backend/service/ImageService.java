package com.autocycle.backend.service;

import java.util.Base64;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.autocycle.backend.config.AiProperties;
import com.autocycle.backend.exception.GenerationException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ImageService implements ProductImageService {

	private final AiProperties properties;
	private final BedrockRuntimeClient bedrock;
	private final ObjectStorage storage;
	private final JsonMapper jsonMapper;

	public ImageService(
			AiProperties properties,
			BedrockRuntimeClient bedrock,
			ObjectStorage storage,
			JsonMapper jsonMapper) {
		this.properties = properties;
		this.bedrock = bedrock;
		this.storage = storage;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public String create(String prompt, String productName) {
		return storage.uploadPng(generate(prompt), productName);
	}

	private byte[] generate(String prompt) {
		try {
			String body = jsonMapper.writeValueAsString(Map.of(
					"taskType", "TEXT_IMAGE",
					"textToImageParams", Map.of("text", prompt),
					"imageGenerationConfig", Map.of(
							"numberOfImages", 1,
							"height", 1024,
							"width", 1024,
							"cfgScale", 8.0)));
			InvokeModelResponse response = bedrock.invokeModel(InvokeModelRequest.builder()
					.modelId(properties.getBedrock().getImageModel())
					.contentType("application/json")
					.accept("application/json")
					.body(SdkBytes.fromUtf8String(body))
					.build());
			JsonNode images = jsonMapper.readTree(response.body().asUtf8String()).path("images");
			if (!images.isArray() || images.isEmpty() || images.get(0).asString("").isBlank()) {
				throw new GenerationException("Bedrock returned no image.");
			}
			return Base64.getDecoder().decode(images.get(0).asString());
		} catch (GenerationException exception) {
			throw exception;
		} catch (BedrockRuntimeException exception) {
			String message = exception.awsErrorDetails() != null ? exception.awsErrorDetails().errorMessage() : exception.getMessage();
			throw new GenerationException(message == null ? "Bedrock image generation failed." : message);
		}
	}
}
