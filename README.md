# Image Archiver

An app to archive images hosted on third party platforms within threads on IPB 4.x forums for future local hosting.  This application was written due to upcoming changes to Imgur's Terms of Service and a desire to preserve old images by hosting them on our own platform.

This application runs in two pieces - an archiver and a swapper.

## Archiver

The archiver parses content from the forums and downloads any images that are found (excluding images already hosted locally and all forum-based emoji) and stores them in directories cataloged by thread ID and post ID.  It also saves the thread content for future swapping, and a CSV which holds corresponding data to the image download and the location on disk that the image was stored.

The archiver requires the following REST endpoints:
* **GET /forums/topics** (user provides forum ID for archival, and the API provides a list of topics.  The application will save the thread ID of each topic)
* **GET /forums/topics/{id}/posts** (user provides ID of topic, and the API provides a list of posts.  The application will save the post ID and thread content, and will also identify every externally hosted image and will attempt to download that image to the local filesystem that is running this application)

The archiver requires the following arguments:
* baseurl - The base URL for the REST API (e.g., https://forums.soa-rs.com/api)
* apikey - The API key generated within the ACP to access the appropriate REST endpoints
* forumid (optional) - In most cases, this will be the single forum ID to archive.
* directory - Path to the local base directory where the images and content should be saved.
* manual (optional)- Manually provide a topic ID to archive.  This argument may be specified multiple times to provide multiple topic IDs.

## Swapper

The swapper (to be written) will take this data, form a URL based on where the images are now hosted, swaps the image location into the content string, and then submits via the REST API the updated content.  It is expected that prior to running, the user has determined the location where the images will be hosted and has placed the images in this location.

The swapper requires the following REST endpoints:
* **POST /forums/posts/{id}** (user provides the updated content string to edit the post with)

The swapper requires the following arguments:
* baseurl - The base URL for the REST API (e.g., https://forums.soa-rs.com/api)
* apikey - The API key generated within the ACP to access the appropriate REST endpoints
* directory - Path to the local base directory where the images and content should be saved.
* imagehosturl - Beginning of the URL to where images have been stored on the remote server.  The path to images stored in the CSV swap file will be appended onto this base path.
