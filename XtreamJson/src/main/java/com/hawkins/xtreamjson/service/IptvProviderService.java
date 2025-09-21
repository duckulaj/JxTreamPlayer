package com.hawkins.xtreamjson.service;

import com.hawkins.xtreamjson.model.IptvProvider;
import com.hawkins.xtreamjson.repository.IptvProviderRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class IptvProviderService {
    
    @Autowired
    private IptvProviderRepository repository;

    public List<IptvProvider> getAllProviders() {
        return repository.findAll();
    }

    public Optional<IptvProvider> getProvider(Long id) {
        return repository.findById(id);
    }

    public IptvProvider saveProvider(IptvProvider provider) {
        return repository.save(provider);
    }

    @Transactional
    public void selectProvider(Long id) {
        log.info("Selecting provider with id: {} (bulk update)", id);
        repository.updateSelectedProvider(id);
        log.info("Provider {} is now selected (bulk update)", id);
    }

    public void deleteProvider(Long id) {
        repository.deleteById(id);
    }

    public Optional<IptvProvider> getSelectedProvider() {
        Optional<IptvProvider> selected = repository.findBySelectedTrue();
        log.info("getSelectedProvider: {}", selected.map(IptvProvider::getId).orElse(null));
        return selected;
    }
}