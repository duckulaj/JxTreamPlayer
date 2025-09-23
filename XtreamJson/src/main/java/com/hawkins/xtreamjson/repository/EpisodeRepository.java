package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.Episode;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
    java.util.List<Episode> findBySeriesIdAndSeasonId(String seriesId, String seasonId);
    java.util.List<Episode> findBySeasonId(String seasonId);
}