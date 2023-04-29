package com.github.soajeff.imagearchiver.rest.fetcher;

import java.util.ArrayList;
import java.util.List;

import com.github.soajeff.imagearchiver.rest.ipb.Results;
import com.github.soajeff.imagearchiver.rest.ipb.TopicResults;
import com.github.soajeff.imagearchiver.rest.ipb.Topic;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;

public class TopicFetcher extends RestFetcher {

	private final Logger logger = LoggerFactory.getLogger(TopicFetcher.class);
	private final int forumId;

	List<Integer> list = new ArrayList<>();

	public TopicFetcher(String baseUrl, String apiKey, int forumId) {
		super(baseUrl, apiKey);
		this.fetchUrl = baseUrl + "forums/topics";
		this.forumId = forumId;
	}

	public List<Integer> getList() {
		return list;
	}

	public void process() {
		TopicResults results = (TopicResults) fetch(1);
		catalogResults(results);

		int totalPages = results.getTotalPages();

		for (int i = 2; i <= totalPages; i++) {
			results = (TopicResults) fetch(i);
			catalogResults(results);
		}
	}

	private void catalogResults(TopicResults results) {
		for (Topic t : results.getResults()) {
			list.add(t.getId());
			logger.trace("Cataloged topic: [" + t.getId() + ", " + t.getTitle() + "]");
		}
	}

	public Results fetch(int page) {
		Client client = ClientBuilder.newClient();
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(this.apiKey, "");
		client.register(feature);
		client.register(JacksonJsonProvider.class);
		WebTarget target = client.target(this.fetchUrl).queryParam("forums", this.forumId).queryParam("page", page).queryParam("perPage", 100);
		logger.trace("Fetching forum results page " + page);
		return target.request(MediaType.APPLICATION_JSON_TYPE).get(TopicResults.class);
	}
}
