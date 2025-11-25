package com.hawkins.xtreamjson.util;

import com.hawkins.xtreamjson.data.LiveStream;
import com.hawkins.xtreamjson.data.MovieStream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StreamUrlHelper {
    public static String buildVodUrl(String apiUrl, String username, String password, MovieStream stream) {
        if (apiUrl == null || apiUrl.isEmpty() ||
                username == null || username.isEmpty() ||
                password == null || password.isEmpty() ||
                stream == null ||
                stream.getContainerExtension() == null || stream.getContainerExtension().isEmpty() ||
                stream.getStreamId() <= 0) { // Check for invalid streamId
            // Return null or throw an exception as appropriate for your application
            return null;
        }
        return String.format("%s/movie/%s/%s/%d.%s",
                apiUrl,
                username,
                password,
                stream.getStreamId(),
                stream.getContainerExtension());
    }

    public static String buildLiveUrl(String apiUrl, String username, String password, LiveStream stream) {
        if (apiUrl == null || apiUrl.isEmpty() ||
                username == null || username.isEmpty() ||
                password == null || password.isEmpty() ||
                stream == null ||
                stream.getStreamId() <= 0) { // Check for invalid streamId
            return null;
        }
        return String.format("%s/live/%s/%s/%d.ts",
                apiUrl,
                username,
                password,
                stream.getStreamId());
    }

    public static String buildEpisodeUrl(String apiUrl, String username, String password, String seriesId,
            String seasonId, String episodeId, String container) {
        if (apiUrl == null || apiUrl.isEmpty() ||
                username == null || username.isEmpty() ||
                password == null || password.isEmpty() ||
                seriesId == null || seriesId.isEmpty() ||
                seasonId == null || seasonId.isEmpty() ||
                episodeId == null || episodeId.isEmpty() ||
                container == null || container.isEmpty()) {
            return null;
        }
        return String.format("%s/series/%s/%s/%s/%s/%s.%s",
                apiUrl,
                username,
                password,
                seriesId,
                seasonId,
                episodeId,
                container);
    }
}