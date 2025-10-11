// PlaylistService.java
package com.hawkins.xtreamjson.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.hawkins.xtreamjson.data.ApplicationProperties;
import com.hawkins.xtreamjson.data.Episode;
import com.hawkins.xtreamjson.data.Season;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.data.SeriesCategory;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesCategoryRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;

@Service
public class PlaylistService {

    private final SeriesCategoryRepository categoryRepository;
    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;
    private final ApplicationPropertiesService applicationPropertiesService;

    public PlaylistService(SeriesCategoryRepository categoryRepository, SeriesRepository seriesRepository, SeasonRepository seasonRepository, EpisodeRepository episodeRepository, ApplicationPropertiesService applicationPropertiesService) {
        this.categoryRepository = categoryRepository;
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
        this.applicationPropertiesService = applicationPropertiesService;
    }

    // Helper to get includedCountries as a Set<String>
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

    // Helper to check if a name matches includedCountries
    private boolean isIncluded(String name, java.util.Set<String> includedSet) {
        if (name == null || name.isEmpty() || includedSet.isEmpty()) return false;
        String upperName = name.toUpperCase();
        for (String prefix : includedSet) {
            if (upperName.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    public String generateFullLibraryPlaylist() {
        StringBuilder playlist = new StringBuilder("#EXTM3U\n");
        AtomicInteger counter = new AtomicInteger(1);

        java.util.Set<String> includedSet = getIncludedCountriesSet();
        // Fetch & sort categories, filter by included country prefix
        List<SeriesCategory> categories = categoryRepository.findAll()
                .stream()
                .filter(cat -> isIncluded(cat.getCategoryName(), includedSet))
                .sorted(Comparator.comparing(SeriesCategory::getCategoryName, String.CASE_INSENSITIVE_ORDER))
                .collect(Collectors.toList());

        for (SeriesCategory category : categories) {
            // Populate seriesList for this category
            List<Series> seriesList = seriesRepository.findByCategoryId(category.getCategoryId());
            category.setSeriesList(seriesList);
            // Category grouping
            playlist.append("\n# ---- Category: ")
                    .append(category.getCategoryName())
                    .append(" ----\n");

            // Sort series inside category
            List<Series> sortedSeries = category.getSeriesList()
                    .stream()
                    .sorted(Comparator.comparing(Series::getName, String.CASE_INSENSITIVE_ORDER))
                    .collect(Collectors.toList());

            for (Series series : sortedSeries) {
                // Populate seasons for this series
                List<Season> seasons = seasonRepository.findBySeriesId(String.valueOf(series.getSeriesId()));
                series.setSeasons(seasons);
                // Series grouping
                playlist.append("# Series: ")
                        .append(series.getName())
                        .append("\n");

                // Sort seasons
                List<Season> sortedSeasons = series.getSeasons()
                        .stream()
                        .sorted(Comparator.comparing(Season::getName))
                        .collect(Collectors.toList());

                for (Season season : sortedSeasons) {
                    // Populate episodes for this season
                    List<Episode> episodes = episodeRepository.findBySeriesIdAndSeasonId(String.valueOf(series.getSeriesId()), season.getSeasonId());
                    season.setEpisodes(episodes);
                    // Season grouping
                    playlist.append("# Season: ")
                            .append(season.getName())
                            .append("\n");

                    // Sort episodes
                    List<Episode> sortedEpisodes = season.getEpisodes()
                            .stream()
                            .sorted(Comparator.comparingInt(e -> Integer.parseInt(e.getEpisodeNum())))
                            .collect(Collectors.toList());

                    for (Episode episode : sortedEpisodes) {
                        int index = counter.getAndIncrement();

                        String displayTitle = String.format("(%03d) [%s | %s | Season %s] S%02dE%02d - %s",
                                index,
                                category.getCategoryName(),
                                series.getName(),
                                season.getName(),
                                season.getSeasonNumber(),
                                Integer.parseInt(episode.getEpisodeNum()),
                                episode.getName());

                        playlist.append("#EXTINF:")
                                .append(episode.getDurationSeconds())
                                .append(", ")
                                .append(displayTitle)
                                .append("\n")
                                .append(episode.getDirectSource())
                                .append("\n");
                    }
                }
            }
        }

        return playlist.toString();
    }
}