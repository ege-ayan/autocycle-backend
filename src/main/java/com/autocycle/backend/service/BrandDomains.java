package com.autocycle.backend.service;

import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BrandDomains {

	private static final Pattern DOMAIN = Pattern.compile(
			"(?i)\\b(?:[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?\\.)+[a-z]{2,24}\\b");

	private BrandDomains() {
	}

	static Optional<String> pick(String brand, String text, List<String> citationDomains) {
		String fromText = first(text);
		if (fromText != null) {
			return Optional.of(fromText);
		}
		String slug = compact(brand);
		if (slug.length() < 2) {
			return Optional.empty();
		}
		for (String citation : citationDomains) {
			String host = host(citation);
			if (host != null && compact(host).contains(slug)) {
				return Optional.of(host);
			}
		}
		return Optional.empty();
	}

	static String host(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		String candidate = value.trim().toLowerCase();
		int scheme = candidate.indexOf("://");
		if (scheme >= 0) {
			candidate = candidate.substring(scheme + 3);
		}
		int slash = candidate.indexOf('/');
		if (slash >= 0) {
			candidate = candidate.substring(0, slash);
		}
		int port = candidate.indexOf(':');
		if (port >= 0) {
			candidate = candidate.substring(0, port);
		}
		if (candidate.startsWith("www.")) {
			candidate = candidate.substring(4);
		}
		if (!DOMAIN.matcher(candidate).matches() || candidate.endsWith(".example")) {
			return null;
		}
		return candidate;
	}

	private static String first(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		Matcher matcher = DOMAIN.matcher(text);
		while (matcher.find()) {
			String host = host(matcher.group());
			if (host != null && !host.endsWith("example.com") && !host.endsWith("example.org")) {
				return host;
			}
		}
		return null;
	}

	private static String compact(String value) {
		if (value == null) {
			return "";
		}
		StringBuilder builder = new StringBuilder();
		for (char character : value.toLowerCase().toCharArray()) {
			if (Character.isLetterOrDigit(character)) {
				builder.append(character);
			}
		}
		return builder.toString();
	}
}
