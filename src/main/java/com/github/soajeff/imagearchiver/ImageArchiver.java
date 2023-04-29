package com.github.soajeff.imagearchiver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.github.soajeff.imagearchiver.rest.fetcher.PostFetcher;
import com.github.soajeff.imagearchiver.rest.fetcher.TopicFetcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageArchiver {

	private String baseUrl;
	private String apiKey;
	private int forumId = 0;
	private File directory;
	private final List<Integer> manualIds = new ArrayList<>();

	public final Logger logger = LoggerFactory.getLogger(ImageArchiver.class);


	public static void main(String[] args) {
		ImageArchiver archiver = new ImageArchiver();
		archiver.logger.info("ImageArchiver Starting...");
		archiver.parseArgs(args);
		List<Integer> topicIds = archiver.getTopics();
		archiver.saveImages(topicIds);

	}

	public void parseArgs(String[] args) {
		int i = 0;
		while (i < args.length) {
			if (args[i].equals("-baseurl")) {
				i++;
				this.baseUrl = args[i];
			} else if (args[i].equals("-apikey")) {
				i++;
				this.apiKey = args[i];
			} else if (args[i].equals("-forumid")) {
				i++;
				this.forumId = Integer.parseInt(args[i]);
			} else if (args[i].equals("-directory")) {
				i++;
				this.directory = new File(args[i]);
				System.out.println(this.directory.getAbsolutePath());
			} else if (args[i].equals("-manual")) {
				i++;
				this.manualIds.add(Integer.parseInt(args[i]));
			}

			i++;
		}
	}

	public List<Integer> getTopics() {
		if (this.manualIds.size() > 0 && this.forumId == 0) {
			return this.manualIds;
		}
		TopicFetcher fetcher = new TopicFetcher(this.baseUrl, this.apiKey, this.forumId);
		fetcher.process();
		List<Integer> tids = fetcher.getList();
		if (this.manualIds.size() > 0)
			tids.addAll(this.manualIds);

		logger.info("Archiver will attempt to archive images from " + tids.size() + " topics");
		return tids;
	}

	public void saveImages(List<Integer> topicIds) {
		PostFetcher fetcher = new PostFetcher(this.baseUrl, this.apiKey, topicIds, this.directory);
		fetcher.process();
	}
}
