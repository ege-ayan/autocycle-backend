package com.autocycle.backend.service;

import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class HttpBrandIconFetcher implements BrandIconFetcher {

	private static final Logger log = LoggerFactory.getLogger(HttpBrandIconFetcher.class);
	private static final int MAX_BYTES = 512 * 1024;

	private final HttpClient httpClient;

	@Autowired
	public HttpBrandIconFetcher() {
		this(HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(4))
				.followRedirects(HttpClient.Redirect.NORMAL)
				.build());
	}

	HttpBrandIconFetcher(HttpClient httpClient) {
		this.httpClient = httpClient;
	}

	@Override
	public Optional<BrandIcon> fetch(String domain) {
		String host = BrandDomains.host(domain);
		if (host == null || !publicHost(host)) {
			return Optional.empty();
		}
		for (String path : new String[] {"/apple-touch-icon.png", "/favicon.ico"}) {
			Optional<BrandIcon> icon = get("https://" + host + path);
			if (icon.isPresent()) {
				return icon;
			}
		}
		return Optional.empty();
	}

	private Optional<BrandIcon> get(String url) {
		try {
			HttpResponse<byte[]> response = httpClient.send(
					HttpRequest.newBuilder(URI.create(url))
							.timeout(Duration.ofSeconds(4))
							.header("User-Agent", "autocycle-backend")
							.GET()
							.build(),
					HttpResponse.BodyHandlers.ofByteArray());
			if (response.statusCode() != 200 || response.body() == null || response.body().length < 32
					|| response.body().length > MAX_BYTES) {
				return Optional.empty();
			}
			String type = response.headers().firstValue("content-type").orElse("");
			int separator = type.indexOf(';');
			if (separator >= 0) {
				type = type.substring(0, separator);
			}
			type = type.trim().toLowerCase();
			String extension = extension(type, url);
			if (extension == null) {
				return Optional.empty();
			}
			if (type.isBlank()) {
				type = "image/" + ("jpg".equals(extension) ? "jpeg" : extension);
			}
			return Optional.of(new BrandIcon(response.body(), type, extension));
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		} catch (Exception exception) {
			log.debug("Brand icon lookup failed for {}: {}", url, exception.getMessage());
			return Optional.empty();
		}
	}

	private static String extension(String contentType, String url) {
		return switch (contentType) {
			case "image/png" -> "png";
			case "image/jpeg", "image/jpg" -> "jpg";
			case "image/webp" -> "webp";
			case "image/svg+xml" -> "svg";
			case "image/x-icon", "image/vnd.microsoft.icon", "image/ico" -> "ico";
			default -> extensionFromPath(url);
		};
	}

	private static String extensionFromPath(String url) {
		String path = url.toLowerCase();
		if (path.endsWith(".png")) {
			return "png";
		}
		if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
			return "jpg";
		}
		if (path.endsWith(".webp")) {
			return "webp";
		}
		if (path.endsWith(".svg")) {
			return "svg";
		}
		if (path.endsWith(".ico")) {
			return "ico";
		}
		return null;
	}

	private static boolean publicHost(String host) {
		if (host.equals("localhost") || host.endsWith(".local") || host.endsWith(".internal")) {
			return false;
		}
		try {
			for (InetAddress address : InetAddress.getAllByName(host)) {
				if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
						|| address.isSiteLocalAddress()) {
					return false;
				}
			}
			return true;
		} catch (Exception exception) {
			return false;
		}
	}
}
