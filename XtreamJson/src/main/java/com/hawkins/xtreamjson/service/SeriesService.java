package com.hawkins.xtreamjson.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.hawkins.xtreamjson.data.Episode;
import com.hawkins.xtreamjson.data.Season;
import com.hawkins.xtreamjson.data.Series;
import com.hawkins.xtreamjson.repository.EpisodeRepository;
import com.hawkins.xtreamjson.repository.SeasonRepository;
import com.hawkins.xtreamjson.repository.SeriesRepository;
import com.hawkins.xtreamjson.util.StreamViewUtils;

@Service
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final SeasonRepository seasonRepository;
    private final EpisodeRepository episodeRepository;

    public SeriesService(SeriesRepository seriesRepository,
            SeasonRepository seasonRepository,
            EpisodeRepository episodeRepository) {
        this.seriesRepository = seriesRepository;
        this.seasonRepository = seasonRepository;
        this.episodeRepository = episodeRepository;
    }

    public List<Series> getSeriesByCategory(String categoryId) {
        return seriesRepository.findByCategoryId(categoryId);
    }

    public List<Season> getSeasonsBySeries(String seriesId) {
        return seasonRepository.findBySeriesId(seriesId);
    }

    public List<Episode> getEpisodesBySeriesAndSeason(String seriesId, String seasonId) {
        return episodeRepository.findBySeriesIdAndSeasonId(seriesId, seasonId);
    }

    public String resolveSeriesImage(String seriesId, String seriesImage) {
        return StreamViewUtils.resolveSeriesImage(seriesId, seriesImage, seriesRepository);
    }
}
