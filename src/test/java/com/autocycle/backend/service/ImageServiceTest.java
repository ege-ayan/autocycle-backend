package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.autocycle.backend.config.AiProperties;
import com.autocycle.backend.exception.GenerationException;

import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.BedrockRuntimeException;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelRequest;
import software.amazon.awssdk.services.bedrockruntime.model.InvokeModelResponse;

class ImageServiceTest {

	private final BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
	private final ObjectStorage storage = mock(ObjectStorage.class);
	private final ImageService images = new ImageService(new AiProperties(), bedrock, storage, tools.jackson.databind.json.JsonMapper.builder().build());

	@Test
	void asksNovaCanvasForOneSquarePngAndStoresIt() {
		byte[] png = {1, 2, 3};
		when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(InvokeModelResponse.builder()
				.body(SdkBytes.fromUtf8String("{\"images\":[\"" + Base64.getEncoder().encodeToString(png) + "\"]}"))
				.build());
		when(storage.uploadPng(any(), eq("Ocean Runner"))).thenReturn("https://cdn.test/ocean.png");

		String url = images.create("a recycled sneaker", "Ocean Runner");

		ArgumentCaptor<InvokeModelRequest> request = ArgumentCaptor.forClass(InvokeModelRequest.class);
		verify(bedrock).invokeModel(request.capture());
		String body = request.getValue().body().asUtf8String();
		assertThat(request.getValue().modelId()).isEqualTo("amazon.nova-canvas-v1:0");
		assertThat(body).contains("TEXT_IMAGE").contains("a recycled sneaker").contains("1024").contains("8.0");
		assertThat(url).isEqualTo("https://cdn.test/ocean.png");
		ArgumentCaptor<byte[]> stored = ArgumentCaptor.forClass(byte[].class);
		verify(storage).uploadPng(stored.capture(), eq("Ocean Runner"));
		assertThat(stored.getValue()).containsExactly(png);
	}

	@Test
	void rejectsAnEmptyImageList() {
		when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenReturn(InvokeModelResponse.builder()
				.body(SdkBytes.fromUtf8String("{\"images\":[]}"))
				.build());

		assertThatThrownBy(() -> images.create("prompt", "Shoe"))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("no image");
	}

	@Test
	void reportsTheBedrockErrorMessage() {
		when(bedrock.invokeModel(any(InvokeModelRequest.class))).thenThrow(
				BedrockRuntimeException.builder().message("throttled").build());

		assertThatThrownBy(() -> images.create("prompt", "Shoe"))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("throttled");
	}
}
