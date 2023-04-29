package com.github.soajeff.imagearchiver.rest.fetcher;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.soajeff.imagearchiver.rest.ipb.PostResults;
import com.github.soajeff.imagearchiver.rest.ipb.Post;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PostFetcher extends RestFetcher {

	private final Logger logger = LoggerFactory.getLogger(PostFetcher.class);
	private final List<Integer> topicIds;
	private final File baseDir;
	private final File imagesBase;
	private final File contentBase;
	private final String domain;

	private final ExecutorService executorService = Executors.newFixedThreadPool(4);
	private final OkHttpClient client = new OkHttpClient();

	public PostFetcher(String baseUrl, String apiKey, List<Integer> topicIds, File baseDir) {
		super(baseUrl, apiKey);
		this.fetchUrl = this.baseUrl + "/forums/topics/";
		this.topicIds = topicIds;
		this.baseDir = baseDir;
		this.imagesBase = new File(this.baseDir, "images");
		this.contentBase = new File(this.baseDir, "content");
		this.domain = this.baseUrl.substring(0, this.baseUrl.indexOf(".com") + 4);
	}

	public void process() {
		this.baseDir.mkdirs();
		this.imagesBase.mkdirs();
		this.contentBase.mkdirs();

		for (int id : topicIds) {
			Runnable runnable = () -> downloadImages(id);

			this.executorService.submit(runnable);
		}

		this.executorService.shutdown();
	}

	private void downloadImages(int id) {
		PostResults results = fetch(1, id);
		int totalPages = results.getTotalPages();

		for (Post p : results.getResults()) {
			analyzePost(p, id);
		}
		if (totalPages > 1) {
			for (int i = 2; i <= totalPages; i++) {
				results = fetch(i, id);
				for (Post p : results.getResults()) {
					analyzePost(p, id);
				}
			}
		}
	}

	private void analyzePost(Post p, int id) {
		//Check if there is images in the document
		logger.trace("Analyzing post id " + p.getId());
		Document document = Jsoup.parse(p.getContent());
		Elements images = document.select("img[src~=(?i)\\.(png|jpe?g|gif)]");
		if (images.size() > 0) {
			//We have images, save stuff.
			boolean download = false;
			for (Element image : images) {
				if (excludeImage(image))
					download = true;
			}
			if (download) {
				logger.trace("[Pid " + p.getId() + "] Has images to download");
				File tid = new File(this.imagesBase, Integer.toString(id));
				File pid = new File(tid, Integer.toString(p.getId()));
				pid.mkdirs();

				for (Element image : images) {
					if (excludeImage(image)) {
						String filename = FilenameUtils.getName(image.attr("src"));
						File imageFile = new File(pid, filename);
						int dupcount = 0;
						while (imageFile.exists()) {
							dupcount++;
							imageFile = new File(pid, dupcount + "-" + filename);
							logger.debug("[Pid " + p.getId() + "] Dup filename, now named " + imageFile.getName());
						}
						File swapFile = new File(pid, p.getId() + "-swap.txt");

						Request request = new Request.Builder().url(image.attr("src")).build();
						try (Response response = client.newCall(request).execute()) {
							if (response.isSuccessful()) {
								assert response.body() != null;
								if (response.body().contentLength() == 0) {
									logger.error("[Pid " + p.getId() + "] URL led to a 0 length item: " + image.attr("src"));
								} else if (!response.body().contentType().toString().startsWith("image/")) {
									logger.error("[Pid " + p.getId() + "] URL led to item that was not an image: " + image.attr("src") + " (instead was " + response.body().contentType() + ")");
								} else {
									try (ByteArrayInputStream bais = new ByteArrayInputStream(
											IOUtils.toByteArray(response.body().byteStream()));
											FileOutputStream fos = new FileOutputStream(imageFile)) {
										IOUtils.copy(bais, fos);
										logger.trace("[Pid " + p.getId() + "] Image " + imageFile.getName()
												+ " downloaded.");

										try (FileWriter fw = new FileWriter(swapFile, true);
												BufferedWriter bw = new BufferedWriter(fw)) {
											bw.write(image.attr("src") + "," + this.baseDir.toURI()
													.relativize(imageFile.toURI()) + "\n");

										} catch (IOException e) {
											logger.error("[Pid " + p.getId() + "] Failed to record image URL in swap file", e);
										}

									} catch (Exception e) {
										logger.error("[Pid " + p.getId() + "] Failed to download image", e);
									}
								}
							} else {
								logger.error("[Pid " + p.getId() + "] URL download was unsuccessful: " + image.attr("src"));
							}

						} catch (Exception e) {
							logger.error("[Pid " + p.getId() + "] Failed to download image: " + e.getMessage());
						}

					}
				}

				// This will need to take into account that there should be at least 2 files, an image plus the swap file.
				if (Objects.requireNonNull(pid.list()).length > 1) {
					File contentFile = new File(this.contentBase, p.getId() + ".txt");
					try (FileWriter fw = new FileWriter(contentFile, false);
							BufferedWriter bw = new BufferedWriter(fw)) {
						bw.write(p.getContent().trim());
					} catch (IOException e) {
						logger.error("Failed to record content in content file: ", e);
					}
				}
			}
		}
	}

	private boolean excludeImage(Element image) {
		boolean download = true;
		if (image.attr("src").startsWith(this.domain))
			download = false;
		else if (image.attr("src").contains("spiritsofarianwyn.com"))
			download = false;
		return download;
	}

	public PostResults fetch(int page, int topicId) {
		Client client = ClientBuilder.newClient();
		HttpAuthenticationFeature feature = HttpAuthenticationFeature.basic(this.apiKey, "");
		client.register(feature);
		client.register(JacksonJsonProvider.class);
		WebTarget target = client.target(this.fetchUrl + topicId + "/posts").queryParam("page", page)
				.queryParam("perPage", 100);
		logger.trace("Fetching forum results page " + page);
		return target.request(MediaType.APPLICATION_JSON_TYPE).get(PostResults.class);
	}

}
