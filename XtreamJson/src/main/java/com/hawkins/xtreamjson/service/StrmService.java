package com.hawkins.xtreamjson.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hawkins.xtreamjson.annotations.TrackExecutionTime;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.util.StreamUrlHelper;

@Service
public class StrmService {
    private static final String MOVIES_DIR = "Movies";

    @Autowired
    private MovieStreamRepository movieStreamRepository;
    
    @Autowired
    private IptvProviderService providerService;

    @Autowired
    private ApplicationPropertiesService applicationPropertiesService;

    @TrackExecutionTime
    public void generateStrmFiles() throws IOException {
    	var selectedProviderOpt = providerService.getSelectedProvider();
        if (selectedProviderOpt.isEmpty()) {
            throw new IllegalStateException("No IPTV provider selected.");
        }
        var selectedProvider = selectedProviderOpt.get();
        Path moviesPath = Paths.get(MOVIES_DIR);
        if (!Files.exists(moviesPath)) {
            Files.createDirectory(moviesPath);
        }

        // Get included countries set
        java.util.Set<String> includedSet = getIncludedCountriesSet();

        List<MovieStream> movies = movieStreamRepository.findAll().stream()
            .filter(movie -> titleIncludesCountry(movie.getName(), includedSet))
            .toList();
        for (MovieStream movie : movies) {
            String safeName = sanitizeFileName(movie.getName(), includedSet, movie.getTmdb());
            Path movieDir = moviesPath.resolve(safeName);
            if (!Files.exists(movieDir)) {
                Files.createDirectory(movieDir);
            }
            String strmFileName = movie.getStreamId() + ".strm";
            Path strmFile = movieDir.resolve(strmFileName);
            // Overwrite file if it already exists
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(strmFile.toFile(), false))) {
            	String url = StreamUrlHelper.buildVodUrl(
                        selectedProvider.getApiUrl(),
                        selectedProvider.getUsername(),
                        selectedProvider.getPassword(),
                        movie
                    );
                writer.write(url != null ? url : "");
            }
        }
    }

    private java.util.Set<String> getIncludedCountriesSet() {
        String included = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
        if (included == null || included.isBlank()) return java.util.Collections.emptySet();
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String s : included.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) set.add(trimmed.toUpperCase());
        }
        return set;
    }

    private boolean titleIncludesCountry(String title, java.util.Set<String> includedSet) {
        if (title == null || title.isEmpty() || includedSet.isEmpty()) return false;
        String upperTitle = title.toUpperCase();
        for (String country : includedSet) {
            if (upperTitle.contains(country)) {
                return true;
            }
        }
        return false;
    }

    private String sanitizeFileName(String name, java.util.Set<String> includedSet, String tmdb) {
        if (name == null) return "unknown";
        String result = name;
        if (includedSet != null && !includedSet.isEmpty()) {
            // Build regex: e.g. ^(4K-)?(EN|NF|US) - \s*
            String countryPattern = String.join("|", includedSet);
            String regex = "^(?:[A-Z0-9]+-)?(" + countryPattern + ") - ";
            result = result.replaceFirst(regex, "");
        }
        // Remove or replace characters not allowed in file/folder names
        result = result.replaceAll("[\\/:*?\"<>|]", "_").trim();
        if (tmdb != null && !tmdb.isBlank()) {
            result += " [tmdbid-" + tmdb + "]";
        }
        return result;
    }
}