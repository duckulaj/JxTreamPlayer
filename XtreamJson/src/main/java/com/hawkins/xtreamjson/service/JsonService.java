package com.hawkins.xtreamjson.service;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hawkins.xtreamjson.data.Episode;
import com.hawkins.xtreamjson.data.LiveCategory;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieCategory;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Season;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.data.SeriesCategory;
import com.hawkins.xtreamjson.data.SeriesInformationToSave;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.repository.MovieCategoryRepository;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesCategoryRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;
import com.hawkins.xtreamjson.util.Constants;
import com.hawkins.xtreamjson.util.XstreamCredentials;
import com.hawkins.xtreamjson.util.XtreamCodesUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class JsonService {
	private final IptvProviderService providerService;
	private final LiveCategoryRepository liveCategoryRepository;    
	private final LiveStreamRepository liveStreamRepository;
	private final MovieCategoryRepository movieCategoryRepository;
	private final MovieStreamRepository movieStreamRepository;
	private final SeriesCategoryRepository seriesCategoryRepository;
	private final SeriesRepository seriesRepository;
	private final SeasonRepository seasonRepository;
	private final EpisodeRepository episodeRepository;
	private static final ObjectMapper objectMapper = new ObjectMapper();
	private static final ExecutorService executor = Executors.newFixedThreadPool(4);

	@Autowired
	public JsonService(IptvProviderService providerService, LiveCategoryRepository liveCategoryRepository, LiveStreamRepository liveStreamRepository, MovieCategoryRepository movieCategoryRepository, MovieStreamRepository movieStreamRepository, SeriesCategoryRepository seriesCategoryRepository, SeriesRepository seriesRepository, SeasonRepository seasonRepository, EpisodeRepository episodeRepository) {
		this.providerService = providerService;
		this.liveCategoryRepository = liveCategoryRepository;
		this.liveStreamRepository = liveStreamRepository;
		this.movieCategoryRepository = movieCategoryRepository;
		this.movieStreamRepository = movieStreamRepository;
		this.seriesCategoryRepository = seriesCategoryRepository;
		this.seriesRepository = seriesRepository;
		this.seasonRepository = seasonRepository;
		this.episodeRepository = episodeRepository;
	}

	public void retreiveJsonData() {
		try {
			// Define base directories

			/* String baseDir = "XStreamJsonFiles";
            String moviesDir = baseDir + "/Movies";
            String liveDir = baseDir + "/Live";
            String seriesDir = baseDir + "/Series";
            // Ensure base directories exist
            File base = new File(baseDir);
            if (!base.exists()) base.mkdirs();
            File movies = new File(moviesDir);
            if (!movies.exists()) movies.mkdirs();
            File live = new File(liveDir);
            if (!live.exists()) live.mkdirs();
            File series = new File(seriesDir);
            if (!series.exists()) series.mkdirs();
			 */

			// Get credentials once
			var providerOpt = providerService.getSelectedProvider();
			if (providerOpt.isEmpty()) {
				log.error("No provider selected. Aborting data retrieval.");
				return;
			}
			XstreamCredentials credentials = new XstreamCredentials(providerOpt.get().getApiUrl(), providerOpt.get().getUsername(), providerOpt.get().getPassword());

			CompletableFuture<Void> liveCategoriesFuture = CompletableFuture.runAsync(() -> {
				try {
					String urlStr = XtreamCodesUtils.buildEndpointUrl(Constants.LIVE_CATEGORIES, credentials);
					URL url = new URI(urlStr).toURL();
					String liveCategoriesJson = httpGetToString(url, credentials);
					urlStr = XtreamCodesUtils.buildEndpointUrl(Constants.LIVE_STREAMS, credentials);
					url = new URI(urlStr).toURL();
					String liveStreamsJson = httpGetToString(url, credentials);
					if (liveCategoriesJson != null) {
						List<LiveCategory> liveCategories = objectMapper.readValue(liveCategoriesJson, objectMapper.getTypeFactory().constructCollectionType(List.class, LiveCategory.class));
						log.info("Live Categories: " + liveCategories.size());
						liveCategoryRepository.saveAll(liveCategories);
					}
					if (liveStreamsJson != null) {
						List<LiveStream> liveStreams = objectMapper.readValue(liveStreamsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, LiveStream.class));
						log.info("Live Streams: " + liveStreams.size());
						liveStreamRepository.saveAll(liveStreams);
					}
				} catch (Exception e) { e.printStackTrace(); }
			}, executor);

			CompletableFuture<Void> movieCategoriesFuture = CompletableFuture.runAsync(() -> {
				try {
					String urlStr = XtreamCodesUtils.buildEndpointUrl(Constants.MOVIE_CATEGORIES, credentials);
					URL url = new URI(urlStr).toURL();
					String movieCategoriesJson = httpGetToString(url, credentials);
					urlStr = XtreamCodesUtils.buildEndpointUrl(Constants.MOVIE_STREAMS, credentials);
					url = new URI(urlStr).toURL();
					String movieStreamsJson = httpGetToString(url, credentials);
					if (movieCategoriesJson != null) {
						List<MovieCategory> movieCategories = objectMapper.readValue(movieCategoriesJson, objectMapper.getTypeFactory().constructCollectionType(List.class, MovieCategory.class));
						log.info("Movie Categories: " + movieCategories.size());
						movieCategoryRepository.saveAll(movieCategories);
					}
					if (movieStreamsJson != null) {
						List<MovieStream> movieStreams = objectMapper.readValue(movieStreamsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, MovieStream.class));
						log.info("Movie Streams: " + movieStreams.size());
						movieStreamRepository.saveAll(movieStreams);
					}
				} catch (Exception e) { e.printStackTrace(); }
			}, executor);

			CompletableFuture<Void> seriesCategoriesFuture = CompletableFuture.runAsync(() -> {
				try {
					List<SeriesCategory> categoriesToSave = new java.util.ArrayList<SeriesCategory>();
					List<Series> seriesToSave = new java.util.ArrayList<Series>();
					List<Season> seasonsToSave = new java.util.ArrayList<Season>();
					List<Episode> episodesToSave = new java.util.ArrayList<Episode>();
					SeriesInformationToSave seriesInformationToSave = new SeriesInformationToSave();

					String urlStr = XtreamCodesUtils.buildEndpointUrl(Constants.SERIES_CATEGORIES, credentials);
					URL url = new URI(urlStr).toURL();
					String seriesCategoriesJson = httpGetToString(url, credentials);
					urlStr = XtreamCodesUtils.buildEndpointUrl(Constants.SERIES, credentials);
					url = new URI(urlStr).toURL();
					String seriesJson = httpGetToString(url, credentials);
					if (seriesCategoriesJson != null) {
						List<SeriesCategory> seriesCategories = objectMapper.readValue(seriesCategoriesJson, objectMapper.getTypeFactory().constructCollectionType(List.class, SeriesCategory.class));
						log.info("Series Categories: " + seriesCategories.size());
						// seriesCategoryRepository.saveAll(seriesCategories);
						seriesInformationToSave.setCategories(seriesCategories);

						List<CompletableFuture<Void>> seriesTasks = new java.util.ArrayList<>();

						for (SeriesCategory sc : seriesCategories) {
							categoriesToSave.add(sc);
							String catId = String.valueOf(sc.getCategoryId());
							if (catId == null || catId.trim().isEmpty() || catId.contains("%")) {
								log.error("Skipping Series Category with invalid ID: '" + catId + "' (" + sc.getCategoryName() + ")");
								continue;
							}

							log.info("Series Category: " + sc.getCategoryName() + " (ID: " + catId + ")");
							String formattedUrl = XtreamCodesUtils.buildEndpointUrl(Constants.SERIES_BY_CATEGORY, credentials, catId);
							URL categoryIdurl = new URI(formattedUrl).toURL();
							String seriesByCategoryJson = httpGetToString(categoryIdurl, credentials);
							if (seriesByCategoryJson != null) {
								List<Series> seriesByCategory = objectMapper.readValue(seriesByCategoryJson, objectMapper.getTypeFactory().constructCollectionType(List.class, Series.class));
								log.info("  Series in Category: " + seriesByCategory.size());
								// seriesRepository.saveAll(seriesByCategory);

								for (Series s : seriesByCategory) {
									seriesToSave.add(s);
									String seriesId = String.valueOf(s.getSeriesId());
									if (seriesId != null && !seriesId.trim().isEmpty()) {

										// Parallelize fetching seasons and episodes
										seriesTasks.add(CompletableFuture.runAsync(() -> {
											try {
												String seriesInfoUrl = XtreamCodesUtils.buildEndpointUrl(Constants.SERIES_INFO, credentials, seriesId);
												URL seriesInfoURL = new URI(seriesInfoUrl).toURL();
												String seriesInfoJson = httpGetToString(seriesInfoURL, credentials);
												if (seriesInfoJson == null) {
													log.error("Failed to fetch series info for seriesId: " + seriesId);
													return;
												}
												// Parse the seasons and episodes in-memory
												try {
													com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(seriesInfoJson);
													if (root.has("episodes") && root.get("episodes").isObject()) {
														com.fasterxml.jackson.databind.JsonNode episodesNode = root.get("episodes");
														java.util.Iterator<String> seasonFields = episodesNode.fieldNames();
														log.info("Series: " + s.getName() + " (ID: " + seriesId + ")");
														while (seasonFields.hasNext()) {
															String seasonNum = XtreamCodesUtils.sanitizeName(seasonFields.next());
															
															com.fasterxml.jackson.databind.JsonNode episodesArray = episodesNode.get(seasonNum);
															if (episodesArray != null && episodesArray.isArray()) {
																// Persist Season entity
																Season seasonEntity = Season.builder()
																		.seasonId(seasonNum)
																		.seriesId(seriesId)
																		.name("Season " + seasonNum)
																		.build();
																seasonsToSave.add(seasonEntity);

																for (com.fasterxml.jackson.databind.JsonNode episodeNode : episodesArray) {

																	String episodeNum = episodeNode.has("episode_num") ? XtreamCodesUtils.sanitizeName(episodeNode.get("episode_num").asText()) : "unknown";
																	// log.info("      Episode: " + episodeNum);
																	// Persist Episode entity
																	Episode episodeEntity = Episode.builder()
																			.episodeId(episodeNode.has("id") ? episodeNode.get("id").asText() : null)
																			.seriesId(seriesId)
																			.seasonId(seasonNum)
																			.name(episodeNode.has("title") ? episodeNode.get("title").asText() : null)
																			.episodeNum(episodeNum)
																			.infoJson(episodeNode.toString())
																			.build();
																	episodesToSave.add(episodeEntity);

																}

															}
														}
													}
												} catch (Exception e) {
													System.err.println("Error parsing seasons/episodes for seriesId: " + seriesId);
													e.printStackTrace();
												}
											} catch (Exception e) {
												System.err.println("Error fetching seasons for seriesId: " + seriesId);
												e.printStackTrace();
											}
										}, executor));
									}
								}
							}
						}
						// Wait for all series/seasons/episodes fetches to complete
						CompletableFuture.allOf(seriesTasks.toArray(new CompletableFuture[0])).join();
						// Finally persist all at once
						if (!categoriesToSave.isEmpty()) {
							seriesInformationToSave.setCategories(categoriesToSave);
						}
						if (!seriesToSave.isEmpty()) {
							seriesInformationToSave.setSeries(seriesToSave);
						}
						if (!seasonsToSave.isEmpty()) {
							seriesInformationToSave.setSeasons(seasonsToSave);
						}
						if (!episodesToSave.isEmpty()) {
							seriesInformationToSave.setEpisodes(episodesToSave);
						}

						seriesCategoryRepository.saveAll(seriesInformationToSave.getCategories());
						seriesRepository.saveAll(seriesInformationToSave.getSeries());
						seasonRepository.saveAll(seriesInformationToSave.getSeasons());
						episodeRepository.saveAll(seriesInformationToSave.getEpisodes());

						log.info("Total Series Categories saved: " + seriesInformationToSave.getCategories().size());
						log.info("Total Series saved: " + seriesInformationToSave.getSeries().size());
						log.info("Total Seasons saved: " + seriesInformationToSave.getSeasons().size());
						log.info("Total Episodes saved: " + seriesInformationToSave.getEpisodes().size());



					}

				} catch (Exception e) { e.printStackTrace(); }
			}, executor);

			CompletableFuture.allOf(liveCategoriesFuture, movieCategoriesFuture, seriesCategoriesFuture).get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		}
		log.info("Finished");
	}

	private static void httpGet(URL url, String fileName, XstreamCredentials credentials) {
		java.net.HttpURLConnection connection = null;
		int responseCode = -1;
		try {
			connection = (java.net.HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(30000);
			// Add Basic Auth header if credentials are present
			if (credentials.getUsername() != null && credentials.getPassword() != null) {
				String auth = credentials.getUsername() + ":" + credentials.getPassword();
				String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
				connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
			}
			responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				String msg = "HTTP error: " + responseCode + " - " + connection.getResponseMessage();
				if (responseCode == 513) {
					msg += " (Custom: Possible authentication or server-side error)";
				}
				log.error("Request to " + url + " failed. " + msg);
				return;
			}
			try (java.io.InputStream is = connection.getInputStream()) {
				// Write JSON to file directly using Jackson for performance
				java.nio.file.Files.copy(is, java.nio.file.Paths.get(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Exception e) {
			log.error("Exception during HTTP GET to " + url + ": " + e.getMessage());
			e.printStackTrace();
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	private static String httpGetToString(URL url, XstreamCredentials credentials) {
		java.net.HttpURLConnection connection = null;
		try {
			connection = (java.net.HttpURLConnection) url.openConnection();
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(30000);
			if (credentials.getUsername() != null && credentials.getPassword() != null) {
				String auth = credentials.getUsername() + ":" + credentials.getPassword();
				String encodedAuth = java.util.Base64.getEncoder().encodeToString(auth.getBytes());
				connection.setRequestProperty("Authorization", "Basic " + encodedAuth);
			}
			int responseCode = connection.getResponseCode();
			if (responseCode != 200) {
				log.error("Request to " + url + " failed. HTTP error: " + responseCode + " - " + connection.getResponseMessage());
				return null;
			}
			try (java.io.InputStream is = connection.getInputStream()) {
				return new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
			}
		} catch (Exception e) {
			log.error("Exception during HTTP GET to " + url + ": " + e.getMessage());
			e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}

	public static <T> List<T> readListFromFile(String filePath, Class<T> clazz) {
		try {
			return objectMapper.readValue(
					Paths.get(filePath).toFile(),
					objectMapper.getTypeFactory().constructCollectionType(List.class, clazz)
					);
		} catch (Exception e) {
			e.printStackTrace();
			return java.util.Collections.emptyList();
		}
	}

	public static <T, R extends JpaRepository<T, ?>> void persistList(R repository, List<T> entities) {
		repository.saveAll(entities);
	}

	public List<MovieCategory> getAllMovieCategories() {
		return movieCategoryRepository.findAll();
	}

	public List<MovieStream> getMoviesByCategory(String categoryId) {
		return movieStreamRepository.findByCategoryId(categoryId);
	}



	public Page<MovieStream> getMoviesByCategory(String categoryId, int page, int size, String letter) {
		CompletableFuture<Page<MovieStream>> future = CompletableFuture.supplyAsync(() -> {
			List<MovieStream> movies = movieStreamRepository.findByCategoryId(categoryId);
			// Filter by letter using cleaned title if needed
			if (letter != null && !letter.isEmpty()) {
				movies = movies.stream()
						.filter(m -> {
							String cleaned = XtreamCodesUtils.cleanTitle(m.getName());
							return !cleaned.isEmpty() && cleaned.substring(0, 1).equalsIgnoreCase(letter);
						})
						.toList();
			}
			// Sort by cleaned title
			movies = movies.stream()
					.sorted(java.util.Comparator.comparing(m -> XtreamCodesUtils.cleanTitle(m.getName()), String.CASE_INSENSITIVE_ORDER))
					.toList();
			// Page manually
			int start = Math.min(page * size, movies.size());
			int end = Math.min(start + size, movies.size());
			List<MovieStream> pageContent = movies.subList(start, end);
			return new org.springframework.data.domain.PageImpl<>(pageContent, PageRequest.of(page, size), movies.size());
		}, executor);
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			log.error("Error in getMoviesByCategory CompletableFuture", e);
			Thread.currentThread().interrupt();
			return Page.empty();
		}
	}

	public List<String> getAvailableStartingLetters(String categoryId) {
		List<MovieStream> movies = movieStreamRepository.findByCategoryId(categoryId);
		java.util.Set<String> letters = new java.util.TreeSet<>();
		for (MovieStream movie : movies) {
			String cleaned = XtreamCodesUtils.cleanTitle(movie.getName());
			if (!cleaned.isEmpty()) {
				letters.add(cleaned.substring(0, 1).toUpperCase());
			}
		}
		return new java.util.ArrayList<>(letters);
	}
}