package com.hawkins.xtreamjson.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.hawkins.xtreamjson.model.IptvProvider;

public interface IptvProviderRepository extends JpaRepository<IptvProvider, Long> {
    Optional<IptvProvider> findBySelectedTrue();

    @Modifying
    @Transactional
    @Query("UPDATE IptvProvider p SET p.selected = CASE WHEN p.id = :id THEN true ELSE false END")
    void updateSelectedProvider(Long id);
}