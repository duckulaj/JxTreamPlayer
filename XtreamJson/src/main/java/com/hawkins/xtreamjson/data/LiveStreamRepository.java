package com.hawkins.xtreamjson.data;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LiveStreamRepository extends JpaRepository<LiveStream, Integer> {
    List<LiveStream> findByCategoryId(String categoryId);
}