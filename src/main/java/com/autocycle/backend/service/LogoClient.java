package com.autocycle.backend.service;

import java.util.Optional;

public interface LogoClient {

	Optional<Logo> find(String brandName);

	record Logo(String logoUrl, String companyDomain) {
	}
}
