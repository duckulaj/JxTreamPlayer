package com.hawkins.xtreamjson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.Series;

public interface SeriesRepository extends JpaRepository<Series, Integer> {
    List<Series> findByCategoryId(String categoryId);
}