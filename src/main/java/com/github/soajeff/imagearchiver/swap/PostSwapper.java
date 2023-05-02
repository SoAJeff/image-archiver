package com.github.soajeff.imagearchiver.swap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import okhttp3.Credentials;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PostSwapper {
	private final String baseUrl;
	private final String postUrl;
	private final String apiKey;
	private final File directory;
	private final String imageHostUrl;
	public final Logger logger = LoggerFactory.getLogger(PostSwapper.class);
	private final ExecutorService executorService = Executors.newFixedThreadPool(4);
	private final OkHttpClient client = new OkHttpClient();

	public PostSwapper(String baseUrl, String apiKey, File directory, String imageHostUrl) {
		this.baseUrl = baseUrl;
		this.postUrl = this.baseUrl + "/forums/posts/";
		this.apiKey = apiKey;
		this.directory = directory;
		this.imageHostUrl = imageHostUrl;
	}

	public void process() {
		File imagesDir = new File(directory, "images");
		if (imagesDir.exists()) {
			File[] threadDirs = imagesDir.listFiles();
			for (File tid : threadDirs) {
				Runnable runnable = () -> processThread(tid);

				this.executorService.submit(runnable);
			}
		}

		this.executorService.shutdown();

	}

	private void processThread(File tid) {
		File[] pidDirs = tid.listFiles();
		for (File pidDir : pidDirs) {
			String pid = pidDir.getName();
			logger.trace("Swapping for PID [" + pid + "]");
			File contentDir = new File(directory, "content");
			File contentFile = new File(contentDir, pid + ".txt");
			File swapFile = new File(pidDir, pid + "-swap.txt");

			if (!contentFile.exists()) {
				logger.error("PID [" + pid
						+ "] Content file does not exist (image download may have failed so nothing to swap)");
			} else if (pidDir.list().length == 0) {
				logger.error(
						"PID [" + pid + "] Pid Directory is empty (image download may have failed so nothing to swap)");
			} else {

				String contents;
				try {
					contents = new String(Files.readAllBytes(contentFile.toPath()));
					try (FileReader fr = new FileReader(swapFile); BufferedReader br = new BufferedReader(fr)) {
						String currentLine;
						while ((currentLine = br.readLine()) != null) {
							String[] line = currentLine.split(",");
							logger.trace("PID [" + pid + "] Swapping " + line[0] + " for " + imageHostUrl + line[1]);
							contents = contents.replace(line[0], imageHostUrl + line[1]);
						}

						//Everything is swapped, now post it.
						postToForum(pid, contents);

					} catch (Exception e) {
						logger.error("Error reading swap file and swapping contents");
					}
				} catch (Exception e) {
					logger.error("Error reading content file, " + e.getMessage());
				}

			}
		}

	}

	public void postToForum(String pid, String content) {
		RequestBody body = new FormBody.Builder().add("post", content).build();
		Request request = new Request.Builder().url(this.postUrl + pid)
				.addHeader("Authorization", Credentials.basic(this.apiKey, "")).post(body).build();
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				logger.error("Error submitting updated content for PID " + pid);
			}
		} catch (Exception e) {
			logger.error("Error with connection", e);
		}
	}
	
}
