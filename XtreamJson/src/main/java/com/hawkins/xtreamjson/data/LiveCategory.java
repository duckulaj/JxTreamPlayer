package com.hawkins.xtreamjson.data;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class LiveCategory {
    @Id
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("category_name")
    private String categoryName;
    @JsonProperty("parent_id")
    private int parentId;

    // Default constructor
    public LiveCategory() {
    }

    // Constructor with parameters
    public LiveCategory(String categoryId, String categoryName, int parentId) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.parentId = parentId;
    }

    // Getters and setters
    public String getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(String categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getParentId() {
        return parentId;
    }

    public void setParentId(int parentId) {
        this.parentId = parentId;
    }

    // toString method for debugging
    @Override
    public String toString() {
        return "LiveCategory{" +
                "categoryId='" + categoryId + '\'' +
                ", categoryName='" + categoryName + '\'' +
                ", parentId=" + parentId +
                '}';
    }
}