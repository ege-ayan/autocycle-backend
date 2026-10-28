package com.autocycle.backend.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@Configuration
@OpenAPIDefinition(
		info = @Info(
				title = "Autocycle API",
				version = "v1",
				description = "Brand collaboration and upcycled product generation"),
		servers = @Server(url = "/", description = "Current server"))
public class OpenApiConfig {
}
