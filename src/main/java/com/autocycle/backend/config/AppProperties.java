package com.autocycle.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private String corsAllowedOrigins = "http://localhost:5173";
	private double similarityThreshold = 0.8;
	private boolean vectorEnabled = true;
	private String s3Region = "eu-central-1";
	private String s3Bucket = "";
	private String s3AccessKeyId = "";
	private String s3SecretAccessKey = "";
	private String s3Endpoint = "";
	private String s3PublicBaseUrl = "";
}
