package com.hawkins.xtreamjson.service;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hawkins.xtreamjson.data.LiveCategory;
import com.hawkins.xtreamjson.data.LiveCategoryRepository;
import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.LiveStreamRepository;
import com.hawkins.xtreamjson.data.MovieCategory;
import com.hawkins.xtreamjson.data.MovieCategoryRepository;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.MovieStreamRepository;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.data.SeriesCategory;
import com.hawkins.xtreamjson.util.Constants;
import com.hawkins.xtreamjson.util.XstreamCredentials;


@Service
public class JsonService {
    private final XstreamCredentials credentials;
    private final LiveCategoryRepository liveCategoryRepository;    
    private final LiveStreamRepository liveStreamRepository;
    private final MovieCategoryRepository movieCategoryRepository;
    private final MovieStreamRepository movieStreamRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final ExecutorService executor = Executors.newFixedThreadPool(4);

    @Autowired
    public JsonService(XstreamCredentials credentials, LiveCategoryRepository liveCategoryRepository, LiveStreamRepository liveStreamRepository, MovieCategoryRepository movieCategoryRepository, MovieStreamRepository movieStreamRepository) {
        this.credentials = credentials;
        this.liveCategoryRepository = liveCategoryRepository;
        this.liveStreamRepository = liveStreamRepository;
        this.movieCategoryRepository = movieCategoryRepository;
        this.movieStreamRepository = movieStreamRepository;
    }

    public void retreiveJsonData() {
        try {
            CompletableFuture<Void> liveCategoriesFuture = CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URI(Constants.LIVE_CATEGORIES).toURL();
                    httpGet(url, "live_categories.json", credentials);
                } catch (Exception e) { e.printStackTrace(); }
            }, executor);

            CompletableFuture<Void> movieCategoriesFuture = CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URI(Constants.MOVIE_CATEGORIES).toURL();
                    httpGet(url, "movie_categories.json", credentials);
                } catch (Exception e) { e.printStackTrace(); }
            }, executor);

            CompletableFuture<Void> seriesCategoriesFuture = CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URI(Constants.SERIES_CATEGORIES).toURL();
                    httpGet(url, "series_categories.json", credentials);
                } catch (Exception e) { e.printStackTrace(); }
            }, executor);

            CompletableFuture<Void> liveStreamsFuture = CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URI(Constants.LIVE_STREAMS).toURL();
                    httpGet(url, "live_stream.json", credentials);
                } catch (Exception e) { e.printStackTrace(); }
            }, executor);

            CompletableFuture<Void> movieStreamsFuture = CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URI(Constants.MOVIE_STREAMS).toURL();
                    httpGet(url, "movie_stream.json", credentials);
                } catch (Exception e) { e.printStackTrace(); }
            }, executor);

            CompletableFuture<Void> seriesFuture = CompletableFuture.runAsync(() -> {
                try {
                    URL url = new URI(Constants.SERIES).toURL();
                    httpGet(url, "series.json", credentials);
                } catch (Exception e) { e.printStackTrace(); }
            }, executor);

            // Wait for all parallel fetches to complete
            CompletableFuture.allOf(liveCategoriesFuture, movieCategoriesFuture, seriesCategoriesFuture, liveStreamsFuture, movieStreamsFuture, seriesFuture).get();

            // Fetch the remaining (likely smaller) endpoints sequentially
            URL url = new URI(Constants.SERIES_BY_CATEGORY).toURL();
            httpGet(url, "series_by_category.json", credentials);

            url = new URI(Constants.SERIES_INFO).toURL();
            httpGet(url, "series_info.json", credentials);

            // Read and print sizes (optional, can be removed for production)
            List<LiveCategory> liveCategories = readListFromFile("live_categories.json", LiveCategory.class);
            System.out.println("Live Categories: " + liveCategories.size());
            liveCategoryRepository.saveAll(liveCategories);

            List<MovieCategory> movieCategories = readListFromFile("movie_categories.json", MovieCategory.class);
            System.out.println("Movie Categories: " + movieCategories.size());
            movieCategoryRepository.saveAll(movieCategories);

            List<SeriesCategory> seriesCategories = readListFromFile("series_categories.json", SeriesCategory.class);
            System.out.println("Series Categories: " + seriesCategories.size());

            List<LiveStream> liveStreams = readListFromFile("live_stream.json", LiveStream.class);
            System.out.println("Live Streams: " + liveStreams.size());
            liveStreamRepository.saveAll(liveStreams);

            List<MovieStream> movieStreams = readListFromFile("movie_stream.json", MovieStream.class);
            System.out.println("Movie Streams: " + movieStreams.size());
            movieStreamRepository.saveAll(movieStreams);

            List<Series> series = readListFromFile("series.json", Series.class);
            System.out.println("Series: " + series.size());

        } catch (MalformedURLException | URISyntaxException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println("Finished");
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
                System.err.println("Request to " + url + " failed. " + msg);
                return;
            }
            try (java.io.InputStream is = connection.getInputStream()) {
                // Write JSON to file directly using Jackson for performance
                java.nio.file.Files.copy(is, java.nio.file.Paths.get(fileName), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            System.err.println("Exception during HTTP GET to " + url + ": " + e.getMessage());
            e.printStackTrace();
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

}