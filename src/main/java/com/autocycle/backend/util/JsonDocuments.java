package com.autocycle.backend.util;

import org.springframework.stereotype.Component;

import com.autocycle.backend.exception.GenerationException;

import tools.jackson.databind.json.JsonMapper;

@Component
public class JsonDocuments {

	private final JsonMapper jsonMapper;

	public JsonDocuments(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public String write(Object value) {
		return jsonMapper.writeValueAsString(value);
	}

	public <T> T read(String content, Class<T> type) {
		try {
			return jsonMapper.readValue(extractObject(content), type);
		} catch (RuntimeException exception) {
			throw new GenerationException("The model returned invalid JSON.", exception);
		}
	}

	static String extractObject(String content) {
		if (content == null) {
			return "";
		}
		String trimmed = content.trim();
		if (trimmed.startsWith("```")) {
			int firstNewline = trimmed.indexOf('\n');
			int lastFence = trimmed.lastIndexOf("```");
			if (firstNewline >= 0 && lastFence > firstNewline) {
				trimmed = trimmed.substring(firstNewline + 1, lastFence).trim();
			}
		}
		int start = trimmed.indexOf('{');
		int end = trimmed.lastIndexOf('}');
		if (start >= 0 && end > start) {
			return trimmed.substring(start, end + 1);
		}
		return trimmed;
	}
}
