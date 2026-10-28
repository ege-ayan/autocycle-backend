package com.autocycle.backend.service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.autocycle.backend.config.AiProperties;
import com.autocycle.backend.exception.GenerationException;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ImageService implements ProductImageService {

	private static final URI ENDPOINT = URI.create("https://generativelanguage.googleapis.com/v1beta/interactions");

	private final AiProperties properties;
	private final ObjectStorage storage;
	private final JsonMapper jsonMapper;
	private final HttpClient httpClient;

	@Autowired
	public ImageService(AiProperties properties, ObjectStorage storage, JsonMapper jsonMapper) {
		this(properties, storage, jsonMapper, HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(10))
				.build());
	}

	ImageService(AiProperties properties, ObjectStorage storage, JsonMapper jsonMapper, HttpClient httpClient) {
		this.properties = properties;
		this.storage = storage;
		this.jsonMapper = jsonMapper;
		this.httpClient = httpClient;
	}

	@Override
	public String create(String prompt, String productName) {
		return storage.upload(generate(prompt), "product-images", productName, "image/jpeg", "jpg");
	}

	private byte[] generate(String prompt) {
		String apiKey = properties.getGemini().getApiKey();
		if (!StringUtils.hasText(apiKey)) {
			throw new GenerationException("Gemini API key is not configured.");
		}
		String body = jsonMapper.writeValueAsString(Map.of(
				"model", properties.getGemini().getImageModel(),
				"input", List.of(Map.of("type", "text", "text", prompt)),
				"response_format", Map.of(
						"type", "image",
						"mime_type", "image/jpeg",
						"aspect_ratio", "1:1",
						"image_size", "1K")));
		HttpRequest request = HttpRequest.newBuilder()
				.uri(ENDPOINT)
				.timeout(Duration.ofSeconds(90))
				.header("Content-Type", "application/json")
				.header("x-goog-api-key", apiKey)
				.POST(HttpRequest.BodyPublishers.ofString(body))
				.build();
		try {
			HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
			if (response.statusCode() != 200) {
				throw new GenerationException(errorMessage(response));
			}
			String encoded = inlineImage(jsonMapper.readTree(response.body()));
			if (encoded.isBlank()) {
				throw new GenerationException("Gemini returned no image.");
			}
			return Base64.getDecoder().decode(encoded);
		} catch (GenerationException exception) {
			throw exception;
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new GenerationException("Gemini image generation was interrupted.");
		} catch (IOException | RuntimeException exception) {
			throw new GenerationException("Gemini image generation failed.");
		}
	}

	private static String inlineImage(JsonNode root) {
		String direct = imageData(root.path("output_image"));
		if (direct.isBlank()) {
			direct = imageData(root.path("outputImage"));
		}
		if (!direct.isBlank()) {
			return direct;
		}
		for (JsonNode step : root.path("steps")) {
			for (JsonNode block : step.path("content")) {
				String data = imageData(block);
				if (!data.isBlank()) {
					return data;
				}
			}
		}
		return "";
	}

	private static String imageData(JsonNode node) {
		String type = node.path("type").asString("");
		if (!type.isEmpty() && !"image".equals(type)) {
			return "";
		}
		return node.path("data").asString("");
	}

	private String errorMessage(HttpResponse<String> response) {
		try {
			String message = jsonMapper.readTree(response.body()).path("error").path("message").asString("");
			if (!message.isBlank()) {
				return message;
			}
		} catch (RuntimeException ignored) {
			// Response body was not the Gemini error envelope.
		}
		return "Gemini image generation failed with status " + response.statusCode() + ".";
	}
}
