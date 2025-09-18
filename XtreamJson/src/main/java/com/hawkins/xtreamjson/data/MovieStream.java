package com.hawkins.xtreamjson.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class MovieStream {
    @Id
    @JsonProperty("stream_id")
    private int streamId;
    @JsonProperty("num")
    private int num;
    @JsonProperty("name")
    private String name;
    @JsonProperty("stream_type")
    private String streamType;
    @JsonProperty("stream_icon")
    private String streamIcon;
    @JsonProperty("rating")
    private String rating;
    @JsonProperty("rating_5based")
    private float rating5Based;
    @JsonProperty("tmdb")
    private String tmdb;
    @JsonProperty("trailer")
    private String trailer;
    @JsonProperty("added")
    private String added;
    @JsonProperty("is_adult")
    private int isAdult;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("category_ids")
    private List<String> categoryIds;
    @JsonProperty("container_extension")
    private String containerExtension;
    @JsonProperty("custom_sid")
    private String customSid;
    @JsonProperty("direct_source")
    private String directSource;

    // Lombok @Data should generate these, but add explicitly to resolve build issues
    public int getStreamId() {
        return streamId;
    }
    public void setStreamId(int streamId) {
        this.streamId = streamId;
    }
    public String getContainerExtension() {
        return containerExtension;
    }
    public void setContainerExtension(String containerExtension) {
        this.containerExtension = containerExtension;
    }
    public String getDirectSource() {
        return directSource;
    }
    public void setDirectSource(String directSource) {
        this.directSource = directSource;
    }
}