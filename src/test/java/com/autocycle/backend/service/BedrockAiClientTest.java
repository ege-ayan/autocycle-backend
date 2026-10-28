package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.autocycle.backend.config.AiProperties;
import com.autocycle.backend.exception.GenerationException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import tools.jackson.databind.json.JsonMapper;

class BedrockAiClientTest {

	private final BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
	private final BedrockAiClient client = new BedrockAiClient(bedrock, new AiProperties(), JsonMapper.builder().build());

	@Test
	void readsTheConverseTextAndTheTitanEmbedding() {
		when(bedrock.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
				.output(ConverseOutput.fromMessage(Message.builder()
						.role(ConversationRole.ASSISTANT)
						.content(ContentBlock.fromText("{\"industry\":\"automotive\"}"))
						.build()))
				.build());
		when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(InvokeModelResponse.builder()
				.body(SdkBytes.fromUtf8String("{\"embedding\":[0.1,0.2]}"))
				.build());

		assertThat(client.complete("system", "user", true)).contains("automotive");
		List<Double> vector = client.embed("BMW");
		assertThat(vector).containsExactly(0.1, 0.2);
		assertThat(client.provider()).isEqualTo("bedrock");
		assertThat(client.embeddingModel()).isEqualTo("amazon.titan-embed-text-v2:0");
	}

	@Test
	void rejectsAnEmptyEmbedding() {
		when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(InvokeModelResponse.builder()
				.body(SdkBytes.fromUtf8String("{\"embedding\":[]}"))
				.build());

		assertThatThrownBy(() -> client.embed("BMW"))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("empty embedding");
	}

	@Test
	void reportsBedrockErrors() {
		when(bedrock.converse(any(ConverseRequest.class))).thenThrow(
				BedrockRuntimeException.builder().message("model overloaded").build());

		assertThatThrownBy(() -> client.complete("system", "user", false))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("overloaded");
	}
}
