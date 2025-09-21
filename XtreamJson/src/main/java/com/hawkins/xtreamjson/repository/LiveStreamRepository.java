package com.hawkins.xtreamjson.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hawkins.xtreamjson.data.LiveStream;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Integer> {
    List<LiveStream> findByCategoryId(String categoryId);
}