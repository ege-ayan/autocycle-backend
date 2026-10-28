package com.autocycle.backend.config;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AsyncConfig {

	@Bean(name = "researchExecutor")
	Executor researchExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}
}
