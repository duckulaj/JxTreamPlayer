package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.LiveCategory;

public interface LiveCategoryRepository extends JpaRepository<LiveCategory, String> {
}
