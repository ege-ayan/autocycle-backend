package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;

import com.autocycle.backend.config.AiProperties;
import com.autocycle.backend.exception.GenerationException;

import tools.jackson.databind.json.JsonMapper;

@SuppressWarnings("unchecked")
class ImageServiceTest {

	private final HttpClient http = mock(HttpClient.class);
	private final ObjectStorage storage = mock(ObjectStorage.class);
	private final ImageService images = new ImageService(properties(), storage, JsonMapper.builder().build(), http);

	@Test
	void asksGeminiForASquarePngAndStoresIt() throws Exception {
		byte[] png = {1, 2, 3};
		stub(200, """
				{"output_image":{"type":"image","mime_type":"image/png","data":"%s"}}
				""".formatted(Base64.getEncoder().encodeToString(png)));
		when(storage.uploadPng(any(), eq("Ocean Runner"))).thenReturn("https://cdn.test/ocean.png");

		String url = images.create("a recycled sneaker", "Ocean Runner");

		ArgumentCaptor<HttpRequest> request = ArgumentCaptor.forClass(HttpRequest.class);
		verify(http).send(request.capture(), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any());
		assertThat(request.getValue().uri().toString()).isEqualTo("https://generativelanguage.googleapis.com/v1beta/interactions");
		assertThat(request.getValue().headers().firstValue("x-goog-api-key")).contains("test-key");
		assertThat(request.getValue().bodyPublisher()).isPresent();
		assertThat(url).isEqualTo("https://cdn.test/ocean.png");
		ArgumentCaptor<byte[]> stored = ArgumentCaptor.forClass(byte[].class);
		verify(storage).uploadPng(stored.capture(), eq("Ocean Runner"));
		assertThat(stored.getValue()).containsExactly(png);
	}

	@Test
	void rejectsAResponseWithoutAnImage() throws Exception {
		stub(200, "{\"steps\":[{\"content\":[{\"type\":\"text\",\"text\":\"no image\"}]}]}");

		assertThatThrownBy(() -> images.create("prompt", "Shoe"))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("no image");
	}

	@Test
	void reportsTheGeminiErrorMessage() throws Exception {
		stub(400, "{\"error\":{\"message\":\"API key not valid.\"}}");

		assertThatThrownBy(() -> images.create("prompt", "Shoe"))
				.isInstanceOf(GenerationException.class)
				.hasMessageContaining("API key not valid");
	}

	private void stub(int status, String body) throws IOException, InterruptedException {
		HttpResponse<String> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(status);
		when(response.body()).thenReturn(body);
		when(http.send(any(HttpRequest.class), ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response);
	}

	private static AiProperties properties() {
		AiProperties properties = new AiProperties();
		properties.getGemini().setApiKey("test-key");
		properties.getGemini().setImageModel("gemini-nano-banana-2.1");
		return properties;
	}
}
