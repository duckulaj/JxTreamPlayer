package com.hawkins.xtreamjson.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hawkins.xtreamjson.data.ApplicationProperties;

@Repository
public interface ApplicationPropertiesRepository extends JpaRepository<ApplicationProperties, Long> {
}
