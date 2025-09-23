package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.Season;

public interface SeasonRepository extends JpaRepository<Season, Long> {
    java.util.List<Season> findBySeriesId(String seriesId);
}