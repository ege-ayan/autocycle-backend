package com.autocycle.backend.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.autocycle.backend.config.AiProperties;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.Citation;
import software.amazon.awssdk.services.bedrockruntime.model.CitationSourceContent;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;
import software.amazon.awssdk.services.bedrockruntime.model.WebLocation;

@Service
public class BedrockGrounding {

	private static final Logger log = LoggerFactory.getLogger(BedrockGrounding.class);
	private static final int MAX_SOURCES = 3;

	private final BedrockRuntimeClient bedrock;
	private final AiProperties properties;

	public BedrockGrounding(BedrockRuntimeClient bedrock, AiProperties properties) {
		this.bedrock = bedrock;
		this.properties = properties;
	}

	public String evidence(String query) {
		if (!StringUtils.hasText(query)) {
			return "";
		}
		try {
			return format(converse(query));
		} catch (RuntimeException exception) {
			log.warn("Brand evidence lookup failed: {}", exception.getMessage());
			return "";
		}
	}

	public Optional<String> officialDomain(String brandName) {
		if (!StringUtils.hasText(brandName)) {
			return Optional.empty();
		}
		try {
			ConverseResponse response = converse(
					"What is the official website domain of the brand \"" + brandName
							+ "\"? Reply with only the domain, such as adidas.com.");
			return BrandDomains.pick(brandName, text(response), domains(response));
		} catch (RuntimeException exception) {
			log.warn("Brand domain lookup failed for {}: {}", brandName, exception.getMessage());
			return Optional.empty();
		}
	}

	private ConverseResponse converse(String prompt) {
		return bedrock.converse(ConverseRequest.builder()
				.modelId(properties.getBedrock().getModel())
				.messages(Message.builder()
						.role(ConversationRole.USER)
						.content(ContentBlock.fromText(prompt))
						.build())
				.build());
	}

	private static String format(ConverseResponse response) {
		StringBuilder evidence = new StringBuilder();
		String body = text(response);
		if (StringUtils.hasText(body)) {
			evidence.append(body.trim());
		}
		int sources = 0;
		for (Citation citation : citations(response)) {
			if (sources >= MAX_SOURCES) {
				break;
			}
			WebLocation web = web(citation);
			String snippet = snippet(citation);
			if (web == null && !StringUtils.hasText(snippet)) {
				continue;
			}
			if (!evidence.isEmpty()) {
				evidence.append('\n');
			}
			if (StringUtils.hasText(citation.title())) {
				evidence.append(citation.title().trim());
			}
			if (StringUtils.hasText(snippet)) {
				if (StringUtils.hasText(citation.title())) {
					evidence.append(" — ");
				}
				evidence.append(snippet);
			}
			if (web != null && StringUtils.hasText(web.url())) {
				evidence.append('\n').append(web.url());
			}
			sources++;
		}
		return evidence.toString().trim();
	}

	private static String text(ConverseResponse response) {
		StringBuilder text = new StringBuilder();
		for (ContentBlock block : content(response)) {
			if (block.text() != null) {
				text.append(block.text());
			}
		}
		return text.toString();
	}

	private static List<String> domains(ConverseResponse response) {
		List<String> domains = new ArrayList<>();
		for (Citation citation : citations(response)) {
			WebLocation web = web(citation);
			if (web != null && StringUtils.hasText(web.domain())) {
				domains.add(web.domain());
			} else if (web != null && StringUtils.hasText(web.url())) {
				domains.add(web.url());
			}
		}
		return domains;
	}

	private static List<Citation> citations(ConverseResponse response) {
		List<Citation> citations = new ArrayList<>();
		for (ContentBlock block : content(response)) {
			if (block.citationsContent() != null && block.citationsContent().hasCitations()) {
				citations.addAll(block.citationsContent().citations());
			}
		}
		return citations;
	}

	private static List<ContentBlock> content(ConverseResponse response) {
		if (response.output() == null || response.output().message() == null || !response.output().message().hasContent()) {
			return List.of();
		}
		return response.output().message().content();
	}

	private static WebLocation web(Citation citation) {
		if (citation.location() == null) {
			return null;
		}
		return citation.location().web();
	}

	private static String snippet(Citation citation) {
		if (!citation.hasSourceContent()) {
			return "";
		}
		StringBuilder snippet = new StringBuilder();
		for (CitationSourceContent source : citation.sourceContent()) {
			if (StringUtils.hasText(source.text())) {
				if (!snippet.isEmpty()) {
					snippet.append(' ');
				}
				snippet.append(source.text().trim());
			}
		}
		return snippet.toString();
	}
}
