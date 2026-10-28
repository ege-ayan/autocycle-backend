package com.autocycle.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.autocycle.backend.config.AiProperties;
import com.autocycle.backend.exception.GenerationException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.SystemContentBlock;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Service
public class BedrockAiClient implements AiClient {

	private final BedrockRuntimeClient bedrock;
	private final AiProperties properties;
	private final JsonMapper jsonMapper;

	public BedrockAiClient(BedrockRuntimeClient bedrock, AiProperties properties, JsonMapper jsonMapper) {
		this.bedrock = bedrock;
		this.properties = properties;
		this.jsonMapper = jsonMapper;
	}

	@Override
	public String complete(String systemPrompt, String userPrompt, boolean json) {
		String user = json
				? userPrompt + "\nReturn only one JSON object and no markdown."
				: userPrompt;
		try {
			ConverseResponse response = bedrock.converse(ConverseRequest.builder()
					.modelId(properties.getBedrock().getModel())
					.system(SystemContentBlock.fromText(systemPrompt))
					.messages(Message.builder()
							.role(ConversationRole.USER)
							.content(ContentBlock.fromText(user))
							.build())
					.build());
			StringBuilder text = new StringBuilder();
			if (response.output() != null && response.output().message() != null) {
				for (ContentBlock block : response.output().message().content()) {
					if (block.text() != null) {
						text.append(block.text());
					}
				}
			}
			if (text.isEmpty()) {
				throw new GenerationException("Bedrock returned an empty response.");
			}
			return text.toString();
		} catch (GenerationException exception) {
			throw exception;
		} catch (BedrockRuntimeException exception) {
			throw new GenerationException(bedrockMessage(exception));
		}
	}

	@Override
	public List<Double> embed(String text) {
		if (text == null || text.isBlank()) {
			throw new GenerationException("Cannot embed an empty text.");
		}
		try {
			String body = jsonMapper.writeValueAsString(Map.of(
					"inputText", text,
					"dimensions", PgVectorIndex.DIMENSIONS,
					"normalize", true));
			InvokeModelResponse response = bedrock.invokeModel(InvokeModelRequest.builder()
					.modelId(embeddingModel())
					.contentType("application/json")
					.accept("application/json")
					.body(SdkBytes.fromUtf8String(body))
					.build());
			JsonNode vector = jsonMapper.readTree(response.body().asUtf8String()).path("embedding");
			if (!vector.isArray() || vector.isEmpty()) {
				throw new GenerationException("Bedrock returned an empty embedding.");
			}
			List<Double> values = new ArrayList<>();
			vector.forEach(item -> values.add(item.asDouble()));
			return values;
		} catch (GenerationException exception) {
			throw exception;
		} catch (BedrockRuntimeException exception) {
			throw new GenerationException(bedrockMessage(exception));
		}
	}

	@Override
	public String provider() {
		return "bedrock";
	}

	@Override
	public String embeddingModel() {
		return properties.getBedrock().getEmbeddingModel();
	}

	private static String bedrockMessage(BedrockRuntimeException exception) {
		if (exception.awsErrorDetails() != null && exception.awsErrorDetails().errorMessage() != null) {
			return exception.awsErrorDetails().errorMessage();
		}
		return exception.getMessage() == null ? "Bedrock request failed." : exception.getMessage();
	}
}
