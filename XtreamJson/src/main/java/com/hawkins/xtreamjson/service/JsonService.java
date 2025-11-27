package com.hawkins.xtreamjson.service;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.hawkins.xtreamjson.annotations.TrackExecutionTime;
import com.hawkins.xtreamjson.data.ApplicationProperties;
import com.hawkins.xtreamjson.data.Episode;
import com.hawkins.xtreamjson.data.LiveCategory;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieCategory;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Season;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.data.SeriesCategory;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.LiveCategoryRepository;
import com.hawkins.xtreamjson.repository.LiveStreamRepository;
import com.hawkins.xtreamjson.repository.MovieCategoryRepository;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesCategoryRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;
import com.hawkins.xtreamjson.util.Constants;
import com.hawkins.xtreamjson.util.StreamUrlHelper;
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
	private final ApplicationPropertiesService applicationPropertiesService;

	private static final int DEFAULT_THREAD_POOL_SIZE = 16;
	private static final ExecutorService executor = Executors.newFixedThreadPool(
			Integer.parseInt(
					System.getenv().getOrDefault("XTREAM_THREAD_POOL_SIZE", String.valueOf(DEFAULT_THREAD_POOL_SIZE))));

	public JsonService(IptvProviderService providerService,
			LiveCategoryRepository liveCategoryRepository,
			LiveStreamRepository liveStreamRepository,
			MovieCategoryRepository movieCategoryRepository,
			MovieStreamRepository movieStreamRepository,
			SeriesCategoryRepository seriesCategoryRepository,
			SeriesRepository seriesRepository,
			SeasonRepository seasonRepository,
			EpisodeRepository episodeRepository,
			ApplicationPropertiesService applicationPropertiesService) {
		this.providerService = providerService;
		this.liveCategoryRepository = liveCategoryRepository;
		this.liveStreamRepository = liveStreamRepository;
		this.movieCategoryRepository = movieCategoryRepository;
		this.movieStreamRepository = movieStreamRepository;
		this.seriesCategoryRepository = seriesCategoryRepository;
		this.seriesRepository = seriesRepository;
		this.seasonRepository = seasonRepository;
		this.episodeRepository = episodeRepository;
		this.applicationPropertiesService = applicationPropertiesService;
	}

	// --- Performance/robustness parameters ---
	private static final Duration BASE_BACKOFF = Duration.ofMillis(250);
	private static final ThreadLocalRandom RAND = ThreadLocalRandom.current();

	private static final ObjectMapper objectMapper = new ObjectMapper();

	// Reuse readers to cut Jackson overhead
	private final ObjectReader liveCategoryReader = objectMapper.readerFor(new TypeReference<List<LiveCategory>>() {
	});
	private final ObjectReader liveStreamReader = objectMapper.readerFor(new TypeReference<List<LiveStream>>() {
	});
	private final ObjectReader movieCategoryReader = objectMapper.readerFor(new TypeReference<List<MovieCategory>>() {
	});
	private final ObjectReader movieStreamReader = objectMapper.readerFor(new TypeReference<List<MovieStream>>() {
	});
	private final ObjectReader seriesCategoryReader = objectMapper.readerFor(new TypeReference<List<SeriesCategory>>() {
	});
	private final ObjectReader seriesListReader = objectMapper.readerFor(new TypeReference<List<Series>>() {
	});

	// Remember permanently-missing series (optional: persist to DB)
	private final java.util.Set<String> knownMissingSeries = ConcurrentHashMap.newKeySet();

	// Status-aware HTTP result
	static final class HttpResult {
		final int status;
		final String body;

		HttpResult(int status, String body) {
			this.status = status;
			this.body = body;
		}

		boolean is2xx() {
			return status >= 200 && status < 300;
		}
	}

	@TrackExecutionTime
	public void retreiveJsonData() {
		try {
			var providerOpt = providerService.getSelectedProvider();
			if (providerOpt.isEmpty()) {
				log.error("No provider selected. Aborting data retrieval.");
				return;
			}
			var p = providerOpt.get();
			var creds = new XstreamCredentials(p.getApiUrl(), p.getUsername(), p.getPassword());

			// Precompute base URLs
			final String liveCatsUrl = XtreamCodesUtils.buildEndpointUrl(Constants.LIVE_CATEGORIES, creds);
			final String liveStreamsUrl = XtreamCodesUtils.buildEndpointUrl(Constants.LIVE_STREAMS, creds);
			final String movieCatsUrl = XtreamCodesUtils.buildEndpointUrl(Constants.MOVIE_CATEGORIES, creds);
			final String movieStreamsUrl = XtreamCodesUtils.buildEndpointUrl(Constants.MOVIE_STREAMS, creds);
			final String seriesCatsUrl = XtreamCodesUtils.buildEndpointUrl(Constants.SERIES_CATEGORIES, creds);

			// Fetch runtime-configurable maxRetries
			final int maxRetries = applicationPropertiesService.getCurrentProperties().getMaxRetries();

			// Before we do anything else we need to clear the database of all existing data

			episodeRepository.deleteAllInBatch();
			seasonRepository.deleteAllInBatch();
			seriesRepository.deleteAllInBatch();
			seriesCategoryRepository.deleteAllInBatch();

			liveStreamRepository.deleteAllInBatch();
			liveCategoryRepository.deleteAllInBatch();

			movieStreamRepository.deleteAllInBatch();
			movieCategoryRepository.deleteAllInBatch();

			knownMissingSeries.clear();
			log.info("Cleared existing data");

			CompletableFuture<Void> liveTask = CompletableFuture.runAsync(() -> {
				try {
					HttpResult cats = getWithRetry(liveCatsUrl, creds, maxRetries);
					HttpResult streams = getWithRetry(liveStreamsUrl, creds, maxRetries);
					if (cats.is2xx() && cats.body != null) {
						List<LiveCategory> list = liveCategoryReader.readValue(cats.body);
						liveCategoryRepository.saveAll(list);
						log.info("Live Categories: {}", list.size());
					}
					if (streams.is2xx() && streams.body != null) {
						List<LiveStream> list = liveStreamReader.readValue(streams.body);
						// Set directSource for each LiveStream using StreamUrlHelper
						for (LiveStream stream : list) {
							String directSourceUrl = StreamUrlHelper.buildLiveUrl(p.getApiUrl(), p.getUsername(),
									p.getPassword(), stream);
							stream.setDirectSource(directSourceUrl);
						}
						liveStreamRepository.saveAll(list);
						log.info("Live Streams: {}", list.size());
					}
				} catch (Exception e) {
					log.warn("Live fetch failed", e);
				}
			}, executor);

			CompletableFuture<Void> movieTask = CompletableFuture.runAsync(() -> {
				try {
					HttpResult cats = getWithRetry(movieCatsUrl, creds, maxRetries);
					HttpResult streams = getWithRetry(movieStreamsUrl, creds, maxRetries);
					if (cats.is2xx() && cats.body != null) {
						List<MovieCategory> list = movieCategoryReader.readValue(cats.body);
						movieCategoryRepository.saveAll(list);
						log.info("Movie Categories: {}", list.size());
					}
					if (streams.is2xx() && streams.body != null) {
						List<MovieStream> list = movieStreamReader.readValue(streams.body);
						movieStreamRepository.saveAll(list);
						log.info("Movie Streams: {}", list.size());
					}
				} catch (Exception e) {
					log.warn("Movie fetch failed", e);
				}
			}, executor);

			CompletableFuture<Void> seriesTask = CompletableFuture
					.runAsync(() -> fetchAndSaveSeries(creds, seriesCatsUrl), executor);

			CompletableFuture.allOf(liveTask, movieTask, seriesTask).join();
			log.info("Finished");
		} catch (Exception e) {
			log.error("Top-level error in retreiveJsonData()", e);
		}
	}

	@TrackExecutionTime
	public void fetchAndSaveSeries(XstreamCredentials creds, String seriesCatsUrl) {
		ApplicationProperties props = applicationPropertiesService.getCurrentProperties();
		final int SERIES_INFO_MAX_INFLIGHT = props.getSeriesInfoMaxInflight();
		final int BATCH_SIZE = props.getBatchSize();
		final int MAX_RETRIES = props.getMaxRetries();

		final var okCount = new AtomicInteger();
		final var notFoundCount = new AtomicInteger();
		final var otherErrorCount = new AtomicInteger();

		// Lock-free aggregators; one saver drains them in chunks
		final var categoriesQ = new ConcurrentLinkedQueue<SeriesCategory>();
		final var seriesQ = new ConcurrentLinkedQueue<Series>();
		final var seasonsQ = new ConcurrentLinkedQueue<Season>();
		final var episodesQ = new ConcurrentLinkedQueue<Episode>();

		// Single saver flushing queues in chunks
		final var stopSaver = new AtomicBoolean(false);
		Thread saver = new Thread(() -> {
			List<SeriesCategory> cBuf = new ArrayList<>(BATCH_SIZE);
			List<Series> sBuf = new ArrayList<>(BATCH_SIZE);
			List<Season> seBuf = new ArrayList<>(BATCH_SIZE);
			List<Episode> eBuf = new ArrayList<>(BATCH_SIZE);

			while (!stopSaver.get() || !categoriesQ.isEmpty() || !seriesQ.isEmpty() || !seasonsQ.isEmpty()
					|| !episodesQ.isEmpty()) {
				drain(categoriesQ, cBuf, BATCH_SIZE, list -> seriesCategoryRepository.saveAll(list));
				drain(seriesQ, sBuf, BATCH_SIZE, list -> seriesRepository.saveAll(list));
				drain(seasonsQ, seBuf, BATCH_SIZE, list -> seasonRepository.saveAll(list));
				drain(episodesQ, eBuf, BATCH_SIZE, list -> episodeRepository.saveAll(list));

				try {
					Thread.sleep(50);
				} catch (InterruptedException ignored) {
				}
			}
			// final flush
			drain(categoriesQ, cBuf, 0, list -> seriesCategoryRepository.saveAll(list));
			drain(seriesQ, sBuf, 0, list -> seriesRepository.saveAll(list));
			drain(seasonsQ, seBuf, 0, list -> seasonRepository.saveAll(list));
			drain(episodesQ, eBuf, 0, list -> episodeRepository.saveAll(list));
		}, "series-saver");
		saver.start();

		long lastLog = System.nanoTime();
		long processed = 0;

		try {
			HttpResult catsRes = getWithRetry(seriesCatsUrl, creds, MAX_RETRIES);
			if (!catsRes.is2xx() || catsRes.body == null) {
				log.warn("Series categories request failed: status {}", catsRes.status);
				return;
			}
			List<SeriesCategory> categories = seriesCategoryReader.readValue(catsRes.body);
			categoriesQ.addAll(categories);

			// Gather unique series IDs across all categories
			final java.util.Set<String> uniqueSeriesIds = new java.util.HashSet<>(8192);

			// Bound parallelism with a semaphore
			final var gate = new Semaphore(SERIES_INFO_MAX_INFLIGHT);
			List<CompletableFuture<Void>> inflight = new ArrayList<>(SERIES_INFO_MAX_INFLIGHT);

			for (SeriesCategory sc : categories) {
				String id = sc.getCategoryId(); // assuming String in your model
				if (id == null || id.isBlank()) {
					log.debug("Skipping Series Category with null/blank ID: {}", sc.getCategoryName());
					continue;
				}
				final String byCatUrl = XtreamCodesUtils.buildEndpointUrl(Constants.SERIES_BY_CATEGORY, creds, id);

				HttpResult seriesRes = getWithRetry(byCatUrl, creds, MAX_RETRIES);
				if (!seriesRes.is2xx() || seriesRes.body == null)
					continue;

				List<Series> inCat = seriesListReader.readValue(seriesRes.body);
				if (inCat == null || inCat.isEmpty())
					continue;

				seriesQ.addAll(inCat);
				for (Series s : inCat) {
					String seriesId = String.valueOf(s.getSeriesId());
					if (seriesId == null || seriesId.isBlank())
						continue;
					if (knownMissingSeries.contains(seriesId))
						continue; // skip known 404s
					uniqueSeriesIds.add(seriesId);
				}
			}

			// Fan-out only unique IDs
			for (String seriesId : uniqueSeriesIds) {
				gate.acquireUninterruptibly();
				var cf = CompletableFuture.runAsync(() -> {
					try {
						fetchOneSeriesInfo(creds, seriesId, seasonsQ, episodesQ, okCount, notFoundCount,
								otherErrorCount, MAX_RETRIES);
					} finally {
						gate.release();
					}
				}, executor);
				inflight.add(cf);

				// keep list from growing unbounded
				if (inflight.size() >= SERIES_INFO_MAX_INFLIGHT * 4) {
					CompletableFuture.anyOf(inflight.toArray(new CompletableFuture[0])).join();
					inflight.removeIf(CompletableFuture::isDone);
				}

				processed++;
				// compact logging every ~2s
				if (System.nanoTime() - lastLog > TimeUnit.SECONDS.toNanos(2)) {
					int infl = SERIES_INFO_MAX_INFLIGHT - gate.availablePermits();
					log.info("Series processed: {} | ok:{} 404:{} other:{} | inflight:{} | queues S:{} Se:{} Ep:{}",
							processed, okCount.get(), notFoundCount.get(), otherErrorCount.get(),
							infl, seriesQ.size(), seasonsQ.size(), episodesQ.size());
					lastLog = System.nanoTime();
				}
			}

			CompletableFuture.allOf(inflight.toArray(new CompletableFuture[0])).join();
		} catch (Exception e) {
			log.warn("Series fetch failed", e);
		} finally {
			stopSaver.set(true);
			try {
				saver.join();
			} catch (InterruptedException ignored) {
			}
			log.info("Series summary — ok:{} 404:{} other:{}", okCount.get(), notFoundCount.get(),
					otherErrorCount.get());
		}
	}

	private static <T> void drain(Queue<T> q, List<T> buf, int threshold, Consumer<List<T>> sink) {
		while (!q.isEmpty() && (threshold == 0 || buf.size() < threshold)) {
			T t = q.poll();
			if (t == null)
				break;
			buf.add(t);
		}
		if (!buf.isEmpty() && (threshold == 0 || buf.size() >= threshold)) {
			sink.accept(List.copyOf(buf));
			buf.clear();
		}
	}

	@TrackExecutionTime
	public void fetchOneSeriesInfo(
			XstreamCredentials creds,
			String seriesId,
			Queue<Season> seasonsQ,
			Queue<Episode> episodesQ,
			AtomicInteger okCount,
			AtomicInteger notFoundCount,
			AtomicInteger otherErrorCount,
			int max404Retries) {
		String url = XtreamCodesUtils.buildEndpointUrl(Constants.SERIES_INFO, creds, seriesId);
		int attempt = 0;
		while (attempt <= max404Retries) {
			try {
				// Add random jitter to avoid spikes
				long jitter = ThreadLocalRandom.current().nextLong(50, 200);
				Thread.sleep(jitter);
			} catch (InterruptedException ignored) {
			}
			HttpResult r = getWithRetry(url, creds, max404Retries);
			if (r.status == 404) {
				if (attempt < max404Retries) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ignored) {
					}
					attempt++;
					continue;
				}
				knownMissingSeries.add(seriesId);
				notFoundCount.incrementAndGet();
				return;
			}
			if (!r.is2xx() || r.body == null || r.body.isBlank()) {
				otherErrorCount.incrementAndGet();
				return;
			}
			try {
				JsonNode root = objectMapper.readTree(r.body);
				JsonNode episodesNode = root.path("episodes");
				if (!episodesNode.isObject()) {
					okCount.incrementAndGet();
					return;
				}
				Iterator<String> seasonFields = episodesNode.fieldNames();
				while (seasonFields.hasNext()) {
					String seasonNumRaw = seasonFields.next();
					String seasonNum = XtreamCodesUtils.sanitizeName(seasonNumRaw);
					JsonNode arr = episodesNode.get(seasonNumRaw);
					if (arr == null || !arr.isArray())
						continue;
					seasonsQ.add(Season.builder()
							.seasonId(seasonNum)
							.seriesId(seriesId)
							.name("Season " + seasonNum)
							.build());
					for (JsonNode ep : arr) {
						String epNum = ep.has("episode_num")
								? XtreamCodesUtils.sanitizeName(ep.get("episode_num").asText())
								: "unknown";
						String episodeId = ep.has("id") ? ep.get("id").asText() : null;
						String containerExtension = ep.has("container_extension")
								? ep.get("container_extension").asText()
								: "mp4";
						String directSource = String.format("%s/series/%s/%s/%s.%s",
								creds.getApiUrl(),
								creds.getUsername(),
								creds.getPassword(),
								episodeId,
								containerExtension);
						episodesQ.add(Episode.builder()
								.episodeId(episodeId)
								.seriesId(seriesId)
								.seasonId(seasonNum)
								.name(ep.has("title") ? ep.get("title").asText() : null)
								.episodeNum(epNum)
								.infoJson(ep.toString())
								.directSource(directSource)
								.build());
					}
				}
				okCount.incrementAndGet();
				return;
			} catch (Exception e) {
				otherErrorCount.incrementAndGet();
				return;
			}
		}
	}

	// ---- Status-aware HTTP with backoff (skips retries on 4xx) ----
	private HttpResult getWithRetry(String urlStr, XstreamCredentials creds, int maxRetries) {
		try {
			URL url = new URI(urlStr).toURL();
			for (int attempt = 0; attempt <= maxRetries; attempt++) {
				try {
					HttpResult r = httpGet(url, creds);
					if (r.status == 404) {
						log.warn("404 Not Found for URL: {} | user: {} | attempt: {}", url, creds.getUsername(),
								attempt);
						return r; // hard miss, don't retry
					}
					if (r.is2xx())
						return r; // success
					if (r.status >= 400 && r.status < 500) {
						log.warn("{} Client Error for URL: {} | user: {} | attempt: {}", r.status, url,
								creds.getUsername(), attempt);
						return r; // other client errors: no retry
					}
					// 5xx or weird -> retry
				} catch (IOException io) {
					// network issue -> retry
				} catch (Exception ex) {
					// transient -> retry
				}
				long jitter = RAND.nextLong(50, 150);
				long sleepMs = Math.min((long) (BASE_BACKOFF.toMillis() * Math.pow(2, attempt)) + jitter, 4000);
				try {
					Thread.sleep(sleepMs);
				} catch (InterruptedException ignored) {
				}
			}
		} catch (Exception e) {
			// bad URL
		}
		return new HttpResult(599, null); // network/unknown
	}

	private HttpResult httpGet(URL url, XstreamCredentials credentials) throws IOException {
		java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
		try {
			connection.setRequestProperty("User-Agent", "Mozilla/5.0");
			connection.setConnectTimeout(10000);
			connection.setReadTimeout(30000);
			// DO NOT set HTTP Basic Authorization header for Xtream Codes API
			int responseCode = connection.getResponseCode();
			java.io.InputStream is = (responseCode >= 200 && responseCode < 300)
					? connection.getInputStream()
					: connection.getErrorStream();
			String body = null;
			if (is != null) {
				try (is) {
					body = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
				}
			}
			return new HttpResult(responseCode, body);
		} finally {
			connection.disconnect();
		}
	}

	public Page<MovieStream> getMoviesByCategory(String categoryId, int page, int size, String letter) {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);

		Specification<MovieStream> spec = StreamSpecifications.<MovieStream>hasCategoryId(categoryId)
				.and(StreamSpecifications.isIncluded(includedSet))
				.and(StreamSpecifications.nameStartsWith(letter));

		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));

		return movieStreamRepository.findAll(spec, pageRequest)
				.map(m -> {
					m.setName(XtreamCodesUtils.cleanTitle(m.getName()));
					return m;
				});
	}

	public List<String> getAvailableStartingLetters(String categoryId) {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		List<MovieStream> movies = movieStreamRepository.findByCategoryId(categoryId);
		java.util.Set<String> letters = new java.util.TreeSet<>();
		for (MovieStream movie : movies) {
			String title = XtreamCodesUtils.cleanTitle(movie.getName());
			if (!title.isEmpty()) {
				String first = title.substring(0, 1).toUpperCase();
				if (includedSet.contains(first)) {
					letters.add(first);
				}
			}
		}
		return new ArrayList<>(letters);
	}

	public List<LiveCategory> getAllLiveCategories() {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		return liveCategoryRepository.findAll().stream()
				.filter(cat -> XtreamCodesUtils.isIncluded(cat.getCategoryName(), includedSet))
				.toList();
	}

	public List<LiveStream> getLiveStreamsByCategory(String categoryId) {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		return liveStreamRepository.findByCategoryId(categoryId).stream()
				.filter(s -> XtreamCodesUtils.isIncluded(s.getName(), includedSet))
				.toList();
	}

	public List<MovieCategory> getAllMovieCategories() {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		return movieCategoryRepository.findAll().stream()
				.filter(cat -> XtreamCodesUtils.isIncluded(cat.getCategoryName(), includedSet))
				.toList();
	}

	public List<MovieStream> getMoviesByCategory(String categoryId) {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		return movieStreamRepository.findByCategoryId(categoryId).stream()
				.filter(m -> XtreamCodesUtils.isIncluded(m.getName(), includedSet))
				.peek(m -> m.setName(XtreamCodesUtils.cleanTitle(m.getName())))
				.toList();
	}

	public List<SeriesCategory> getAllSeriesCategories() {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		return seriesCategoryRepository.findAll().stream()
				.filter(cat -> XtreamCodesUtils.isIncluded(cat.getCategoryName(), includedSet))
				.toList();
	}

	public List<Series> getSeriesByCategory(String categoryId) {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		return seriesRepository.findByCategoryId(categoryId).stream()
				.filter(s -> XtreamCodesUtils.isIncluded(s.getName(), includedSet))
				.toList();
	}

	public org.springframework.data.domain.Page<Series> getSeriesByCategory(String categoryId, int page, int size,
			String letter) {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);

		Specification<Series> spec = StreamSpecifications.<Series>hasCategoryId(categoryId)
				.and(StreamSpecifications.isIncluded(includedSet))
				.and(StreamSpecifications.nameStartsWith(letter));

		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));

		return seriesRepository.findAll(spec, pageRequest);
	}

	public java.util.List<String> getAvailableSeriesStartingLetters(String categoryId) {
		java.util.Set<String> includedSet = XtreamCodesUtils.getIncludedCountriesSet(applicationPropertiesService);
		java.util.List<Series> seriesList = seriesRepository.findByCategoryId(categoryId);
		java.util.Set<String> letters = new java.util.TreeSet<>();
		for (Series s : seriesList) {
			String cleaned = com.hawkins.xtreamjson.util.XtreamCodesUtils.cleanTitle(s.getName());
			if (!cleaned.isEmpty()) {
				String first = cleaned.substring(0, 1).toUpperCase();
				if (includedSet.contains(first)) {
					letters.add(first);
				}
			}
		}
		return new java.util.ArrayList<>(letters);
	}

	public List<Season> getSeasonsBySeries(String seriesId) {
		return seasonRepository.findBySeriesId(seriesId);
	}

	public List<Episode> getEpisodesBySeason(String seasonId) {
		return episodeRepository.findBySeasonId(seasonId);
	}

	public List<MovieStream> searchMoviesByTitle(String q) {
		if (q == null || q.isBlank())
			return List.of();
		try {
			List<MovieStream> results = movieStreamRepository.searchByNameContaining(q);
			results.forEach(m -> m.setName(XtreamCodesUtils.cleanTitle(m.getName())));
			return results;
		} catch (Exception e) {
			log.warn("searchMoviesByTitle failed for '{}': {}", q, e.getMessage());
			return List.of();
		}
	}

	public List<Series> searchSeriesByTitle(String q) {
		if (q == null || q.isBlank())
			return List.of();
		try {
			return seriesRepository.searchByNameContaining(q);
		} catch (Exception e) {
			log.warn("searchSeriesByTitle failed for '{}': {}", q, e.getMessage());
			return List.of();
		}
	}

}
