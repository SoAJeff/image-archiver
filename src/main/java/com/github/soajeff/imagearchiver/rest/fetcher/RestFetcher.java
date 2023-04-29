package com.github.soajeff.imagearchiver.rest.fetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestFetcher {

	protected final String baseUrl;
	protected final String apiKey;
	protected String fetchUrl;

	private final Logger logger = LoggerFactory.getLogger(RestFetcher.class);

	protected RestFetcher(String baseUrl, String apiKey) {
		this.baseUrl = baseUrl;
		this.apiKey = apiKey;
	}


}
