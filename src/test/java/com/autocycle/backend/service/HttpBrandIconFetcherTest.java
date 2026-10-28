package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class HttpBrandIconFetcherTest {

	private final HttpClient httpClient = mock(HttpClient.class);
	private final HttpBrandIconFetcher fetcher = new HttpBrandIconFetcher(httpClient);

	@Test
	void downloadsTheTouchIconAndKeepsTheContentType() throws Exception {
		byte[] png = new byte[32];
		png[0] = 1;
		HttpResponse<byte[]> downloaded = response(200, png, "image/png; charset=binary");
		when(httpClient.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
				.thenReturn(downloaded);

		BrandIconFetcher.BrandIcon icon = fetcher.fetch("https://www.adidas.com").orElseThrow();

		assertThat(icon.bytes()).isEqualTo(png);
		assertThat(icon.contentType()).isEqualTo("image/png");
		assertThat(icon.extension()).isEqualTo("png");
		verify(httpClient).send(org.mockito.ArgumentMatchers.<HttpRequest>argThat(request ->
				request.uri().getPath().equals("/apple-touch-icon.png")
						&& request.headers().firstValue("User-Agent").orElse("").equals("autocycle-backend")),
				org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
	}

	@Test
	void fallsBackToTheFaviconWhenTheTouchIconIsMissing() throws Exception {
		byte[] ico = new byte[40];
		when(httpClient.send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any()))
				.thenAnswer(invocation -> {
			HttpRequest request = invocation.getArgument(0);
			if (request.uri().getPath().endsWith(".png")) {
				return response(404, new byte[0], "");
			}
			return response(200, ico, "image/x-icon");
		});

		BrandIconFetcher.BrandIcon icon = fetcher.fetch("adidas.com").orElseThrow();

		assertThat(icon.extension()).isEqualTo("ico");
		assertThat(icon.contentType()).isEqualTo("image/x-icon");
	}

	@Test
	void skipsPrivateAndUnresolvableHosts() throws Exception {
		assertThat(fetcher.fetch("localhost")).isEmpty();
		assertThat(fetcher.fetch("printer.local")).isEmpty();
		assertThat(fetcher.fetch("files.internal")).isEmpty();
		assertThat(fetcher.fetch("missing.invalid")).isEmpty();
		assertThat(fetcher.fetch("")).isEmpty();
		verify(httpClient, never()).send(any(HttpRequest.class), org.mockito.ArgumentMatchers.<HttpResponse.BodyHandler<byte[]>>any());
	}

	private static HttpResponse<byte[]> response(int status, byte[] body, String type) {
		@SuppressWarnings("unchecked")
		HttpResponse<byte[]> response = mock(HttpResponse.class);
		when(response.statusCode()).thenReturn(status);
		when(response.body()).thenReturn(body);
		Map<String, List<String>> headers = type.isBlank() ? Map.of() : Map.of("content-type", List.of(type));
		when(response.headers()).thenReturn(HttpHeaders.of(headers, (name, value) -> true));
		return response;
	}
}
