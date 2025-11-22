package com.hawkins.xtreamjson.service;

import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.hawkins.xtreamjson.data.ApplicationProperties;
import com.hawkins.xtreamjson.repository.ApplicationPropertiesRepository;

@Service
public class ApplicationPropertiesService {
    private final ApplicationPropertiesRepository repository;

    public ApplicationPropertiesService(ApplicationPropertiesRepository repository) {
        this.repository = repository;
    }

    public ApplicationProperties getCurrentProperties() {
        // Always return the first (or create default if none)
        return repository.findTopByOrderByIdAsc().orElseGet(() -> {
            ApplicationProperties props = new ApplicationProperties();
            return repository.save(props);
        });
    }

    @Transactional
    public ApplicationProperties updateProperties(ApplicationProperties updated) {
        if (updated.getId() == null) {
            return repository.save(updated);
        }
        Optional<ApplicationProperties> existingOpt = repository.findById(updated.getId());
        if (existingOpt.isPresent()) {
            ApplicationProperties existing = existingOpt.get();
            existing.setSeriesInfoMaxInflight(updated.getSeriesInfoMaxInflight());
            existing.setBatchSize(updated.getBatchSize());
            existing.setMaxRetries(updated.getMaxRetries());
            existing.setIncludedCountries(updated.getIncludedCountries());
            return repository.save(existing);
        } else {
            return repository.save(updated);
        }
    }

    public void deleteProperties(Long id) {
        repository.deleteById(id);
    }

    public java.util.List<ApplicationProperties> getAll() {
        return repository.findAll();
    }
}