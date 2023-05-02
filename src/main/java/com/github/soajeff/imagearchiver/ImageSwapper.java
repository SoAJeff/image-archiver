package com.github.soajeff.imagearchiver;

import java.io.File;

import com.github.soajeff.imagearchiver.swap.PostSwapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImageSwapper {

	private String baseUrl;
	private String apiKey;
	private File directory;
	private String imageHostUrl;
	public final Logger logger = LoggerFactory.getLogger(ImageSwapper.class);

	public static void main(String[] args) {
		ImageSwapper swapper = new ImageSwapper();
		swapper.logger.info("Imageswapper starting...");
		swapper.parseArgs(args);
		swapper.swap();
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
			} else if (args[i].equals("-directory")) {
				i++;
				this.directory = new File(args[i]);
			} else if (args[i].equals("-imagehosturl")) {
				i++;
				this.imageHostUrl = args[i];
			}

			i++;
		}
	}

	private void swap() {
		PostSwapper swapper = new PostSwapper(this.baseUrl, this.apiKey, this.directory, this.imageHostUrl);
		swapper.process();
	}
}
