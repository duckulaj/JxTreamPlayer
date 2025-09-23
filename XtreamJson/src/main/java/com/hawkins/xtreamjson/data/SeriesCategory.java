package com.hawkins.xtreamjson.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import lombok.Data;

@Data
@Entity
public class SeriesCategory {
    @Id
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("category_name")
    private String categoryName;
    @JsonProperty("parent_id")
    private int parentId;

    @Transient
    private List<Series> seriesList;

    public SeriesCategory() {}

    public SeriesCategory(String categoryId, String categoryName, int parentId) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.parentId = parentId;
    }
}