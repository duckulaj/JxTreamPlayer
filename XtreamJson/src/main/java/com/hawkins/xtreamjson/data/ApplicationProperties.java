package com.hawkins.xtreamjson.data;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class ApplicationProperties {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private int seriesInfoMaxInflight = 8;
    private int batchSize = 500;
    private int maxRetries = 5;

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getSeriesInfoMaxInflight() { return seriesInfoMaxInflight; }
    public void setSeriesInfoMaxInflight(int v) { this.seriesInfoMaxInflight = v; }

    public int getBatchSize() { return batchSize; }
    public void setBatchSize(int v) { this.batchSize = v; }

    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int v) { this.maxRetries = v; }
}
