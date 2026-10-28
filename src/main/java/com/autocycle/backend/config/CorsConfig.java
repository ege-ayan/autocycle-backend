package com.autocycle.backend.config;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

	private final String allowedOrigins;

	public CorsConfig(
			@Value("${app.cors-allowed-origins:http://localhost:5173}") String allowedOrigins) {
		this.allowedOrigins = allowedOrigins;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		String[] origins = Arrays.stream(allowedOrigins.split(","))
				.map(String::trim)
				.filter(origin -> !origin.isEmpty())
				.toArray(String[]::new);
		boolean wildcard = Arrays.asList(origins).contains("*");
		var mapping = registry.addMapping("/**")
				.allowedMethods("GET", "POST", "OPTIONS")
				.allowedHeaders("*");
		if (wildcard) {
			mapping.allowedOriginPatterns("*");
		} else {
			mapping.allowedOrigins(origins).allowCredentials(true);
		}
	}
}
