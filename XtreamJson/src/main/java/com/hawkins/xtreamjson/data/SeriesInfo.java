package com.hawkins.xtreamjson.data;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class SeriesInfo {
    @JsonProperty("seasons")
    private List<Object> seasons;
    @JsonProperty("info")
    private Info info;
    @JsonProperty("episodes")
    private Map<String, List<Episode>> episodes;

    @Data
    public static class Info {
        @JsonProperty("name")
        private String name;
        @JsonProperty("cover")
        private String cover;
        @JsonProperty("plot")
        private String plot;
        @JsonProperty("series_cast")
        private String seriesCast;
        @JsonProperty("director")
        private String director;
        @JsonProperty("genre")
        private String genre;
        @JsonProperty("releaseDate")
        private String releaseDate;
        @JsonProperty("release_date")
        private String release_date;
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
        // Getters and setters omitted for brevity
    }

    @Data
    public static class Episode {
        @JsonProperty("id")
        private String id;
        @JsonProperty("episode_num")
        private int episodeNum;
        @JsonProperty("title")
        private String title;
        @JsonProperty("container_extension")
        private String containerExtension;
        @JsonProperty("info")
        private EpisodeInfo info;
        // Getters and setters omitted for brevity
    }

    @Data
    public static class EpisodeInfo {
        @JsonProperty("air_date")
        private String airDate;
        @JsonProperty("rating")
        private int rating;
        @JsonProperty("id")
        private int id;
        @JsonProperty("movie_image")
        private String movieImage;
        @JsonProperty("duration_secs")
        private int durationSecs;
        @JsonProperty("duration")
        private String duration;
        @JsonProperty("video")
        private Object video;
        // Getters and setters omitted for brevity
    }
}