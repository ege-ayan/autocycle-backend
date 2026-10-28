package com.autocycle.backend.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.autocycle.backend.config.AppProperties;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketConfiguration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Slf4j
@Component
public class LocalBucketInitializer implements ApplicationRunner {

	private final S3Client s3Client;
	private final AppProperties properties;

	public LocalBucketInitializer(S3Client s3Client, AppProperties properties) {
		this.s3Client = s3Client;
		this.properties = properties;
	}

	@Override
	public void run(ApplicationArguments args) {
		if (!StringUtils.hasText(properties.getS3Endpoint()) || !StringUtils.hasText(properties.getS3Bucket())) {
			return;
		}
		String bucket = properties.getS3Bucket();
		try {
			s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
		} catch (NoSuchBucketException exception) {
			create(bucket);
		} catch (S3Exception exception) {
			if (exception.statusCode() == 404) {
				create(bucket);
			} else {
				log.warn("Could not check local bucket {}: {}", bucket, exception.getMessage());
			}
		} catch (RuntimeException exception) {
			log.warn("Could not check local bucket {}: {}", bucket, exception.getMessage());
		}
	}

	private void create(String bucket) {
		try {
			CreateBucketRequest.Builder request = CreateBucketRequest.builder().bucket(bucket);
			if (!"us-east-1".equals(properties.getS3Region())) {
				request.createBucketConfiguration(CreateBucketConfiguration.builder()
						.locationConstraint(BucketLocationConstraint.fromValue(properties.getS3Region()))
						.build());
			}
			s3Client.createBucket(request.build());
			log.info("Created local bucket {}", bucket);
		} catch (RuntimeException exception) {
			log.warn("Could not create local bucket {}: {}", bucket, exception.getMessage());
		}
	}
}
