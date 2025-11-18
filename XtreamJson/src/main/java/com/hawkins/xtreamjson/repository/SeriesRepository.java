package com.hawkins.xtreamjson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hawkins.xtreamjson.data.Series;

public interface SeriesRepository extends JpaRepository<Series, Integer> {
    List<Series> findByCategoryId(String categoryId);

    @Query("SELECT s FROM Series s WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    List<Series> searchByNameContaining(@Param("q") String q);
}