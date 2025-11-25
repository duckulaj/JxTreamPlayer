package com.hawkins.xtreamjson.service;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hawkins.xtreamjson.annotations.TrackExecutionTime;
import com.hawkins.xtreamjson.data.Episode;
import com.hawkins.xtreamjson.data.MovieStream;
import com.hawkins.xtreamjson.data.Season;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.MovieStreamRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;
import com.hawkins.xtreamjson.util.StreamUrlHelper;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class StrmService {
    private static final String MOVIES_DIR = "Movies";

    @Autowired
    private MovieStreamRepository movieStreamRepository;

    @Autowired
    private IptvProviderService providerService;

    @Autowired
    private ApplicationPropertiesService applicationPropertiesService;

    @Autowired
    private SeriesRepository seriesRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private EpisodeRepository episodeRepository;

    // Create a counter for Movies and Shows generated
    AtomicInteger movieCounter = new AtomicInteger(0);
    AtomicInteger showCounter = new AtomicInteger(0);

    public void generateAllStrmFiles() throws IOException {
        generateMovieFolders();
        generateShowFolders();
        log.info("Generated {} movie folders.", movieCounter.get());
        log.info("Generated {} shows.", showCounter.get());
    }

    @TrackExecutionTime
    public void generateMovieFolders() throws IOException {
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

        log.info("Generating .strm files for {} movies matching included countries.", movies.size());

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
                        movie);
                writer.write(url != null ? url : "");
            }
            movieCounter.incrementAndGet();
        }
    }

    public void generateShowFolders() throws IOException {
        final String SHOWS_DIR = "Shows";
        Path showsPath = Paths.get(SHOWS_DIR);
        if (!Files.exists(showsPath)) {
            Files.createDirectory(showsPath);
        }

        // Get included countries set
        java.util.Set<String> includedSet = getIncludedCountriesSet();

        List<Series> seriesList = seriesRepository.findAll().stream()
                .filter(series -> titleIncludesCountry(series.getName(), includedSet))
                .toList();
        // List<Series> seriesList = seriesRepository.findAll();

        log.info("Generating .strm files for {} series matching included countries.", seriesList.size());

        for (Series series : seriesList) {
            showCounter.incrementAndGet();

            String seriesFolderName = sanitizeFileName(series.getName(), null, null);

            Path seriesPath = showsPath.resolve(seriesFolderName);
            if (!Files.exists(seriesPath)) {
                Files.createDirectory(seriesPath);
            }
            // Get all seasons for this series
            List<Season> seasons = seasonRepository.findBySeriesId(String.valueOf(series.getSeriesId()));
            for (Season season : seasons) {
                String seasonFolderName = "Season " + season.getSeasonNumber();
                Path seasonPath = seriesPath.resolve(seasonFolderName);
                if (!Files.exists(seasonPath)) {
                    Files.createDirectory(seasonPath);
                }
                // Get all episodes for this season
                List<Episode> episodes = episodeRepository
                        .findBySeriesIdAndSeasonId(String.valueOf(series.getSeriesId()), season.getSeasonId());
                for (Episode episode : episodes) {
                    String episodeNum = padNumber(episode.getEpisodeNum(), 2);
                    String seasonNum = padNumber(String.valueOf(season.getSeasonNumber()), 2);
                    String episodeTitle = sanitizeFileName(episode.getName(), null, null);
                    String fileName = String.format("%s%s S%sE%s %s.strm",
                            seriesFolderName,
                            "",
                            seasonNum,
                            episodeNum,
                            episodeTitle);
                    Path episodeFile = seasonPath.resolve(fileName);
                    // Write directSource to the .strm file
                    try (BufferedWriter writer = new BufferedWriter(new FileWriter(episodeFile.toFile(), false))) {
                        writer.write(episode.getDirectSource() != null ? episode.getDirectSource() : "");
                    }
                }
            }
        }
    }

    private java.util.Set<String> getIncludedCountriesSet() {
        String included = applicationPropertiesService.getCurrentProperties().getIncludedCountries();
        if (included == null || included.isBlank())
            return java.util.Collections.emptySet();
        java.util.Set<String> set = new java.util.HashSet<>();
        for (String s : included.split(",")) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty())
                set.add(trimmed.toUpperCase());
        }
        return set;
    }

    private boolean titleIncludesCountry(String title, java.util.Set<String> includedSet) {
        if (title == null || title.isEmpty() || includedSet.isEmpty())
            return false;
        String upperTitle = title.toUpperCase();
        for (String country : includedSet) {
            if (upperTitle.contains(country)) {
                return true;
            }
        }
        return false;
    }

    private String padNumber(String num, int length) {
        if (num == null)
            return "00";
        try {
            int n = Integer.parseInt(num.replaceAll("\\D", ""));
            return String.format("%0" + length + "d", n);
        } catch (Exception e) {
            return "00";
        }
    }

    private String sanitizeFileName(String name, java.util.Set<String> includedSet, String tmdb) {
        if (name == null)
            return "Unknown";
        // Remove illegal characters for file/folder names
        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_");
        safe = safe.replaceAll("[\n\r]", " ").trim();
        // Optionally add TMDB id if present
        if (tmdb != null && !tmdb.isBlank()) {
            safe += " [tmdbid-" + tmdb + "]";
        }
        return safe;
    }
}