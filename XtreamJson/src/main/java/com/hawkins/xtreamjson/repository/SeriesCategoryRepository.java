package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.SeriesCategory;

public interface SeriesCategoryRepository extends JpaRepository<SeriesCategory, String> {
}
