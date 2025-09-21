package com.hawkins.xtreamjson.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hawkins.xtreamjson.data.MovieStream;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieStreamRepository extends JpaRepository<MovieStream, Integer> {
    List<MovieStream> findByCategoryId(String categoryId);

    Page<MovieStream> findByCategoryId(String categoryId, Pageable pageable);

    @Query("SELECT m FROM MovieStream m WHERE m.categoryId = :categoryId AND LOWER(m.name) LIKE LOWER(CONCAT(:letter, '%'))")
    Page<MovieStream> findByCategoryIdAndNameStartingWith(@Param("categoryId") String categoryId, @Param("letter") String letter, Pageable pageable);

    @Query("SELECT DISTINCT UPPER(SUBSTRING(m.name, 1, 1)) FROM MovieStream m WHERE m.categoryId = :categoryId ORDER BY UPPER(SUBSTRING(m.name, 1, 1))")
    List<String> findDistinctFirstLettersByCategoryId(@Param("categoryId") String categoryId);
}