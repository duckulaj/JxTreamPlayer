package com.hawkins.xtreamjson.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Series {
    @Id
    @JsonProperty("series_id")
    private int seriesId;
    @JsonProperty("num")
    private int num;
    @JsonProperty("name")
    private String name;
    @JsonProperty("cover")
    private String cover;
    @JsonProperty("plot")
    @Column(columnDefinition = "TEXT")
    private String plot;
    @JsonProperty("series_cast")
    @JsonAlias("cast")
    private String seriesCast;
    @JsonProperty("director")
    private String director;
    @JsonProperty("genre")
    private String genre;
    @JsonProperty("releaseDate")
    @JsonAlias("release_date")
    private String releaseDate;
    @JsonProperty("last_modified")
    private String lastModified;
    @JsonProperty("rating")
    private String rating;
    @JsonProperty("rating_5based")
    private String rating5Based;
    @JsonProperty("backdrop_path")
    private List<String> backdropPath;
    @JsonProperty("tmdb")
    private String tmdb;
    @JsonProperty("youtube_trailer")
    private String youtubeTrailer;
    @JsonProperty("episode_run_time")
    private String episodeRunTime;
    @JsonProperty("category_id")
    private String categoryId;
    @JsonProperty("category_ids")
    private List<Integer> categoryIds;
}