package com.autocycle.backend.config;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class AwsConfig {

	@Bean
	S3Client s3Client(AppProperties properties) {
		var builder = S3Client.builder().region(Region.of(properties.getS3Region()));
		if (hasKeys(properties)) {
			builder.credentialsProvider(credentials(properties));
		}
		if (StringUtils.hasText(properties.getS3Endpoint())) {
			builder.endpointOverride(URI.create(properties.getS3Endpoint()));
			builder.serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
		}
		return builder.build();
	}

	@Bean
	BedrockRuntimeClient bedrockRuntimeClient(AppProperties properties) {
		var builder = BedrockRuntimeClient.builder().region(Region.of(properties.getS3Region()));
		if (hasKeys(properties)) {
			builder.credentialsProvider(credentials(properties));
		}
		return builder.build();
	}

	private static boolean hasKeys(AppProperties properties) {
		return StringUtils.hasText(properties.getS3AccessKeyId())
				&& StringUtils.hasText(properties.getS3SecretAccessKey());
	}

	private static StaticCredentialsProvider credentials(AppProperties properties) {
		return StaticCredentialsProvider.create(AwsBasicCredentials.create(
				properties.getS3AccessKeyId(), properties.getS3SecretAccessKey()));
	}
}
