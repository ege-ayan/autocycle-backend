package com.autocycle.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.autocycle.backend.config.AppProperties;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

class S3ObjectStorageTest {

	@Test
	void uploadsAPngAndReturnsThePublicUrl() {
		S3Client s3 = org.mockito.Mockito.mock(S3Client.class);
		when(s3.putObject(any(PutObjectRequest.class), any(RequestBody.class))).thenReturn(PutObjectResponse.builder().build());
		AppProperties properties = new AppProperties();
		properties.setS3Bucket("autocycle-product-images");
		properties.setS3Region("eu-central-1");
		properties.setS3PublicBaseUrl("https://cdn.example.com/images");
		S3ObjectStorage storage = new S3ObjectStorage(s3, properties);

		String url = storage.uploadPng(new byte[] {1, 2, 3}, "Ocean Runner");

		ArgumentCaptor<PutObjectRequest> captor = ArgumentCaptor.forClass(PutObjectRequest.class);
		verify(s3).putObject(captor.capture(), any(RequestBody.class));
		assertThat(captor.getValue().bucket()).isEqualTo("autocycle-product-images");
		assertThat(captor.getValue().contentType()).isEqualTo("image/png");
		assertThat(captor.getValue().key()).startsWith("product-images/Ocean_Runner_").endsWith(".png");
		assertThat(url).startsWith("https://cdn.example.com/images/product-images/Ocean_Runner_");
	}
}
