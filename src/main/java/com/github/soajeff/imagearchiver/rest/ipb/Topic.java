package com.github.soajeff.imagearchiver.rest.ipb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Topic {
	private int id;
	private String title;

	public int getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}
}
