package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.MovieCategory;

public interface MovieCategoryRepository extends JpaRepository<MovieCategory, String> {
}
