package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.DefaultApplicationArguments;

import com.autocycle.backend.config.AppProperties;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

class LocalBucketInitializerTest {

	private final S3Client s3 = mock(S3Client.class);
	private final AppProperties properties = new AppProperties();
	private final LocalBucketInitializer initializer = new LocalBucketInitializer(s3, properties);
	private final DefaultApplicationArguments args = new DefaultApplicationArguments(new String[0]);

	@Test
	void doesNothingWithoutALocalEndpoint() {
		properties.setS3Bucket("autocycle-product-images");

		initializer.run(args);

		verify(s3, never()).headBucket(any(HeadBucketRequest.class));
	}

	@Test
	void leavesAnExistingBucketAlone() {
		localBucket();
		when(s3.headBucket(any(HeadBucketRequest.class))).thenReturn(HeadBucketResponse.builder().build());

		initializer.run(args);

		verify(s3, never()).createBucket(any(CreateBucketRequest.class));
	}

	@Test
	void createsAMissingBucketInTheConfiguredRegion() {
		localBucket();
		when(s3.headBucket(any(HeadBucketRequest.class))).thenThrow(NoSuchBucketException.builder().message("missing").build());

		initializer.run(args);

		ArgumentCaptor<CreateBucketRequest> request = ArgumentCaptor.forClass(CreateBucketRequest.class);
		verify(s3).createBucket(request.capture());
		assertThat(request.getValue().bucket()).isEqualTo("autocycle-product-images");
		assertThat(request.getValue().createBucketConfiguration().locationConstraint())
				.isEqualTo(BucketLocationConstraint.EU_CENTRAL_1);
	}

	@Test
	void createsTheBucketWhenHeadReturnsNotFound() {
		localBucket();
		properties.setS3Region("us-east-1");
		when(s3.headBucket(any(HeadBucketRequest.class)))
				.thenThrow(S3Exception.builder().statusCode(404).message("missing").build());

		initializer.run(args);

		ArgumentCaptor<CreateBucketRequest> request = ArgumentCaptor.forClass(CreateBucketRequest.class);
		verify(s3).createBucket(request.capture());
		assertThat(request.getValue().createBucketConfiguration()).isNull();
	}

	@Test
	void keepsGoingWhenTheBucketCheckFails() {
		localBucket();
		when(s3.headBucket(any(HeadBucketRequest.class)))
				.thenThrow(S3Exception.builder().statusCode(500).message("down").build());

		initializer.run(args);

		verify(s3, never()).createBucket(any(CreateBucketRequest.class));
	}

	private void localBucket() {
		properties.setS3Endpoint("http://localhost:4566");
		properties.setS3Bucket("autocycle-product-images");
		properties.setS3Region("eu-central-1");
	}
}
