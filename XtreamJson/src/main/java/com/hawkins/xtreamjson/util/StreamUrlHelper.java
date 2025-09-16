package com.hawkins.xtreamjson.util;

import com.hawkins.xtreamjson.data.MovieStream;

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
                stream.getContainerExtension()
        );
    }
}