package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.LiveCategory;

import java.util.Collection;
import java.util.List;

public interface LiveCategoryRepository extends JpaRepository<LiveCategory, String> {
    List<LiveCategory> findByCategoryNameIn(Collection<String> names);
}