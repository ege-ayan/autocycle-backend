package com.autocycle.backend.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.autocycle.backend.config.AppProperties;
import com.autocycle.backend.exception.GenerationException;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3ObjectStorage implements ObjectStorage {

	private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private final S3Client s3Client;
	private final AppProperties properties;

	public S3ObjectStorage(S3Client s3Client, AppProperties properties) {
		this.s3Client = s3Client;
		this.properties = properties;
	}

	@Override
	public String uploadPng(byte[] png, String productName) {
		return upload(png, "product-images", productName, "image/png", "png");
	}

	@Override
	public String upload(byte[] body, String folder, String name, String contentType, String extension) {
		if (!StringUtils.hasText(properties.getS3Bucket())) {
			throw new GenerationException("AWS_S3_BUCKET is not configured.");
		}
		String key = folder + "/" + safe(name) + "_" + STAMP.format(LocalDateTime.now()) + "_"
				+ UUID.randomUUID().toString().substring(0, 8) + "." + extension;
		s3Client.putObject(PutObjectRequest.builder()
				.bucket(properties.getS3Bucket())
				.key(key)
				.contentType(contentType)
				.build(), RequestBody.fromBytes(body));
		return publicUrl(key);
	}

	private String publicUrl(String key) {
		if (StringUtils.hasText(properties.getS3PublicBaseUrl())) {
			return strip(properties.getS3PublicBaseUrl()) + "/" + key;
		}
		if (StringUtils.hasText(properties.getS3Endpoint())) {
			return strip(properties.getS3Endpoint()) + "/" + properties.getS3Bucket() + "/" + key;
		}
		return "https://" + properties.getS3Bucket() + ".s3." + properties.getS3Region() + ".amazonaws.com/" + key;
	}

	private static String safe(String productName) {
		String source = productName == null ? "product" : productName;
		StringBuilder builder = new StringBuilder();
		for (char character : source.toCharArray()) {
			if (Character.isLetterOrDigit(character) || character == '-' || character == '_') {
				builder.append(character);
			} else if (Character.isWhitespace(character)) {
				builder.append('_');
			}
		}
		String value = builder.toString();
		return value.isBlank() ? "product" : value;
	}

	private static String strip(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}
}
