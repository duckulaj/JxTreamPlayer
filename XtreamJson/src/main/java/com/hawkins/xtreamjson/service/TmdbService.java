package com.hawkins.xtreamjson.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TmdbService {

    @Value("${tmdb.api.key:}")
    private String tmdbApiKey;

    private static final String TMDB_BASE_URL = "https://api.themoviedb.org/3";
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public TmdbService() {
        this.objectMapper = new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    @Cacheable("tmdbSummaries")
    public String getMovieSummary(String tmdbId) {
        if (tmdbId == null || tmdbId.isBlank() || "0".equals(tmdbId)) {
            return null;
        }

        if (tmdbApiKey == null || tmdbApiKey.isBlank()) {
            log.warn("TMDB API Key is not configured.");
            return "TMDB API Key not configured.";
        }

        try {
            return fetchSummaryFromApi(tmdbId);
        } catch (Exception e) {
            log.error("Failed to fetch TMDB summary for ID: " + tmdbId, e);
            return "Error fetching summary.";
        }
    }

    private String fetchSummaryFromApi(String tmdbId) throws Exception {
        String url = TMDB_BASE_URL + "/movie/" + tmdbId + "?api_key=" + tmdbApiKey + "&language=en-US";
        log.info("Fetching TMDB summary for ID: " + tmdbId);
        log.info("URL: " + url);

        HttpRequest request = HttpRequest.newBuilder()
                .GET()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            JsonNode root = objectMapper.readTree(response.body());
            if (root.has("overview")) {
                String overview = root.get("overview").asText();
                if (overview == null || overview.isBlank()) {
                    log.warn("TMDB Overview is null or blank for ID: {}", tmdbId);
                    return "No overview provided by TMDB.";
                }
                return overview;
            } else {
                log.warn("TMDB response missing 'overview' field for ID: {}", tmdbId);
            }
        } else if (response.statusCode() == 404) {
            log.warn("Movie not found in TMDB with ID: {}", tmdbId);
            return "Summary not available.";
        } else {
            log.error("TMDB API Error: Status Code {}", response.statusCode());
        }

        return null;
    }
}
