package com.hawkins.xtreamjson.data;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SeriesInfo(
        @JsonProperty("seasons") List<Object> seasons,
        @JsonProperty("info") Info info,
        @JsonProperty("episodes") Map<String, List<Episode>> episodes) {
    public record Info(
            @JsonProperty("name") String name,
            @JsonProperty("cover") String cover,
            @JsonProperty("plot") String plot,
            @JsonProperty("series_cast") String seriesCast,
            @JsonProperty("director") String director,
            @JsonProperty("genre") String genre,
            @JsonProperty("releaseDate") String releaseDate,
            @JsonProperty("release_date") String release_date,
            @JsonProperty("last_modified") String lastModified,
            @JsonProperty("rating") String rating,
            @JsonProperty("rating_5based") String rating5Based,
            @JsonProperty("backdrop_path") List<String> backdropPath,
            @JsonProperty("tmdb") String tmdb,
            @JsonProperty("youtube_trailer") String youtubeTrailer,
            @JsonProperty("episode_run_time") String episodeRunTime,
            @JsonProperty("category_id") String categoryId,
            @JsonProperty("category_ids") List<Integer> categoryIds) {
    }

    public record Episode(
            @JsonProperty("id") String id,
            @JsonProperty("episode_num") int episodeNum,
            @JsonProperty("title") String title,
            @JsonProperty("container_extension") String containerExtension,
            @JsonProperty("info") EpisodeInfo info) {
    }

    public record EpisodeInfo(
            @JsonProperty("air_date") String airDate,
            @JsonProperty("rating") int rating,
            @JsonProperty("id") int id,
            @JsonProperty("movie_image") String movieImage,
            @JsonProperty("duration_secs") int durationSecs,
            @JsonProperty("duration") String duration,
            @JsonProperty("video") Object video) {
    }
}