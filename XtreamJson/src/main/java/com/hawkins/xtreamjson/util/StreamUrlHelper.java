package com.hawkins.xtreamjson.util;

import com.hawkins.xtreamjson.data.MovieStream;

public class StreamUrlHelper {
    public static String buildVodUrl(String apiUrl, String username, String password, MovieStream stream) {
        if (stream.getContainerExtension() == null || stream.getContainerExtension().isEmpty()) {
            return null;
        }
        return String.format("%s/vod/%s/%s/%d.%s",
                apiUrl,
                username,
                password,
                stream.getStreamId(),
                stream.getContainerExtension()
        );
    }
}
