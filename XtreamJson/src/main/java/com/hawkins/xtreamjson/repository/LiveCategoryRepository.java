package com.hawkins.xtreamjson.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.LiveCategory;

public interface LiveCategoryRepository extends JpaRepository<LiveCategory, String> {
    List<LiveCategory> findByCategoryNameIn(Collection<String> names);
}