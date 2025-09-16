package com.hawkins.xtreamjson.data;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieStreamRepository extends JpaRepository<MovieStream, Integer> {
    List<MovieStream> findByCategoryId(String categoryId);
}