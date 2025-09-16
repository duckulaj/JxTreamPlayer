package com.hawkins.xtreamjson.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Integer> {
    List<LiveStream> findByCategoryId(String categoryId);
}