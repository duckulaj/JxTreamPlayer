package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.Series;

import java.util.List;

public interface SeriesRepository extends JpaRepository<Series, Integer> {
    List<Series> findByCategoryId(String categoryId);
}