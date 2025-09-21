package com.hawkins.xtreamjson.repository;

import com.hawkins.xtreamjson.data.Episode;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {
}
