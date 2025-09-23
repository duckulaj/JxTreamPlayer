package com.hawkins.xtreamjson.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hawkins.xtreamjson.model.IptvProvider;
import com.hawkins.xtreamjson.service.IptvProviderService;

@RestController
@RequestMapping("/api/providers")
public class IptvProviderController {
    @Autowired
    private IptvProviderService service;

    @GetMapping
    public List<IptvProvider> getAllProviders() {
        return service.getAllProviders();
    }

    @GetMapping("/{id}")
    public ResponseEntity<IptvProvider> getProvider(@PathVariable Long id) {
        return service.getProvider(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public IptvProvider addProvider(@RequestBody IptvProvider provider) {
        provider.setId(null); // Ensure new
        return service.saveProvider(provider);
    }

    @PutMapping("/{id}")
    public ResponseEntity<IptvProvider> updateProvider(@PathVariable Long id, @RequestBody IptvProvider provider) {
        return service.getProvider(id)
                .map(existing -> {
                    existing.setApiUrl(provider.getApiUrl());
                    existing.setUsername(provider.getUsername());
                    existing.setPassword(provider.getPassword());
                    return ResponseEntity.ok(service.saveProvider(existing));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteProvider(@PathVariable Long id) {
        service.deleteProvider(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/select/{id}")
    public ResponseEntity<Void> selectProvider(@PathVariable Long id) {
        if (service.getProvider(id).isPresent()) {
            service.selectProvider(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/selected")
    public ResponseEntity<IptvProvider> getSelectedProvider() {
        return service.getSelectedProvider()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
