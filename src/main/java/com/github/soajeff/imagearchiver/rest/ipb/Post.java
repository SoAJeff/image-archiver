package com.github.soajeff.imagearchiver.rest.ipb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)

public class Post {
	private int id;
	private String content;
	private String url;

	public int getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public String getUrl() {
		return url;
	}
}
