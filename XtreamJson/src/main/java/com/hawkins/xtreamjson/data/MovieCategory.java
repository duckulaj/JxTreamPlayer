package com.hawkins.xtreamjson.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class MovieCategory {
    @Id
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("category_name")
    private String categoryName;
    @JsonProperty("parent_id")
    private int parentId;

    public MovieCategory() {
    }

    public MovieCategory(String categoryId, String categoryName, int parentId) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.parentId = parentId;
    }
}