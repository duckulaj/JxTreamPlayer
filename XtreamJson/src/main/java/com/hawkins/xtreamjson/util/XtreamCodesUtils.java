package com.hawkins.xtreamjson.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XtreamCodesUtils {

    private static String createPlayableUrl(String type, String streamId, String extension, XstreamCredentials credentials) {
        String API_URL = credentials.getApiUrl();
        String USERNAME = credentials.getUsername();
        String PASSWORD = credentials.getPassword();
        StringBuilder url = new StringBuilder();
        url.append(API_URL).append("/")
        .append(type).append("/")
        .append(USERNAME).append("/")
        .append(PASSWORD).append("/")
        .append(streamId);
        switch (type) {
        case "movie":
            if (extension != null && !extension.isEmpty()) {
                if (!extension.startsWith(".")) url.append(".");
                url.append(extension);
            } else {
                url.append(".mp4");
            }
            break;
        case "series":
            url.append(".mkv");
            break;
        case "live":
            url.append(".ts");
            break;
        default:
            log.warn("Unknown type for stream URL: {}", type);
            break;
        }
        return url.toString();
    }

}