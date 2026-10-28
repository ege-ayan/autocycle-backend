package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.autocycle.backend.config.AiProperties;

import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.Citation;
import software.amazon.awssdk.services.bedrockruntime.model.CitationLocation;
import software.amazon.awssdk.services.bedrockruntime.model.CitationSourceContent;
import software.amazon.awssdk.services.bedrockruntime.model.CitationsContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ContentBlock;
import software.amazon.awssdk.services.bedrockruntime.model.ConversationRole;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseOutput;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseRequest;
import software.amazon.awssdk.services.bedrockruntime.model.ConverseResponse;
import software.amazon.awssdk.services.bedrockruntime.model.Message;

class BedrockGroundingTest {

	private final BedrockRuntimeClient bedrock = mock(BedrockRuntimeClient.class);
	private final BedrockGrounding grounding = new BedrockGrounding(bedrock, new AiProperties());

	@Test
	void evidenceIncludesTheAnswerAndTheCitedPage() {
		when(bedrock.converse(any(ConverseRequest.class))).thenReturn(ConverseResponse.builder()
				.output(ConverseOutput.fromMessage(Message.builder()
						.role(ConversationRole.ASSISTANT)
						.content(
								ContentBlock.fromText("Adidas uses recycled polyester."),
								ContentBlock.fromCitationsContent(CitationsContentBlock.builder()
										.citations(Citation.builder()
												.title("Adidas")
												.sourceContent(CitationSourceContent.fromText("ocean plastic"))
												.location(CitationLocation.fromWeb(web -> web
														.url("https://www.adidas.com/sustainability")
														.domain("adidas.com")))
												.build())
										.build()))
						.build()))
				.build());

		ArgumentCaptor<ConverseRequest> request = ArgumentCaptor.forClass(ConverseRequest.class);
		String evidence = grounding.evidence("adidas sustainability");

		org.mockito.Mockito.verify(bedrock).converse(request.capture());
		assertThat(request.getValue().modelId()).isEqualTo("eu.amazon.nova-pro-v1:0");
		assertThat(request.getValue().toolConfig()).isNull();
		assertThat(evidence).contains("recycled polyester");
		assertThat(evidence).contains("https://www.adidas.com/sustainability");
		assertThat(evidence).contains("ocean plastic");
	}

	@Test
	void returnsEmptyEvidenceWhenGroundingFails() {
		when(bedrock.converse(any(ConverseRequest.class))).thenThrow(new RuntimeException("timeout"));

		assertThat(grounding.evidence("adidas")).isEmpty();
		assertThat(grounding.officialDomain("Adidas")).isEmpty();
	}
}
